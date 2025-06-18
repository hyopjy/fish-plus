package fish.plus.mirai.plugin.mqtt;

import fish.plus.mirai.plugin.manager.RodeoManager;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MqttClientStart {
    private static final String BROKER = "tcp://127.0.0.1:1883";
    private static final String CLIENT_ID = "JavaMqttClient";
    private static final List<String> SUBSCRIBED_TOPICS = new ArrayList<>();
    private static MqttClientStart instance;
    
    // 添加重连成功回调
    private Runnable onReconnectSuccessCallback;
    
    private MqttClient mqttClient;
    private MqttConnectOptions connOpts;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final AtomicBoolean maxAttemptsReached = new AtomicBoolean(false);
    private Thread listeningThread;
    
    // 重连配置
    private static final int MAX_RECONNECT_ATTEMPTS = 5; // 减少最大重连次数
    private static final int INITIAL_RECONNECT_DELAY = 3000; // 3秒，给服务器更多启动时间
    private static final int MAX_RECONNECT_DELAY = 20000; // 20秒
    private static final int CONNECTION_CHECK_INTERVAL = 10000; // 10秒

    private MqttClientStart() {
        initializeMqttClient();
    }

    private void initializeMqttClient() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            mqttClient = new MqttClient(BROKER, CLIENT_ID, persistence);
            
            // 配置连接选项
            connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(false);
            connOpts.setAutomaticReconnect(false); // 禁用自动重连，手动控制
            connOpts.setKeepAliveInterval(60); // 增加心跳间隔到60秒，与服务端匹配
            connOpts.setConnectionTimeout(15); // 增加连接超时时间
            connOpts.setMaxInflight(1000); // 增加最大并发消息数
            connOpts.setUserName(""); // 如果需要用户名
            connOpts.setPassword("".toCharArray()); // 如果需要密码
            
            // 设置回调
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("MQTT连接丢失: " + (cause != null ? cause.getMessage() : "未知原因"));
                    connected.set(false);
                    
                    // 记录连接丢失的详细信息
                    if (cause != null) {
                        System.err.println("连接丢失原因: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
                        cause.printStackTrace();
                    }
                    
                    // 连接丢失时清理已订阅主题列表，确保重连后能重新订阅
                    System.out.println("连接丢失，清理已订阅主题列表，等待重连后重新订阅");
                    
                    // 不在这里重连，让监听线程处理
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("收到消息: " + topic + " " + new String(message.getPayload()));
                    
                    String messageStr = new String(message.getPayload());
                    System.out.println("处理消息: " + messageStr);
                    
                    try {
                        Long groupId = Long.parseLong(messageStr);
                        System.out.println("解析群ID成功: " + groupId);
                        RodeoManager.init(groupId);
                        System.out.println("RodeoManager初始化完成");
                    } catch (NumberFormatException e) {
                        System.err.println("消息格式错误，无法解析为群ID: " + messageStr);
                        System.err.println("错误详情: " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("处理消息时发生异常: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("消息发送完成");
                }
            });

            // 启动监听线程
            startListeningThread();
            
        } catch (MqttException e) {
            System.err.println("初始化MQTT客户端失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized MqttClientStart getInstance() {
        if (instance == null) {
            instance = new MqttClientStart();
        }
        return instance;
    }

    private void startListeningThread() {
        listeningThread = new Thread(() -> {
            long lastHeartbeat = System.currentTimeMillis();
            long lastConnectionCheck = System.currentTimeMillis();
            
            while (running.get()) {
                try {
                    long currentTime = System.currentTimeMillis();
                    
                    // 检查连接状态
                    boolean currentlyConnected = isConnected();
                    
                    if (!currentlyConnected && !maxAttemptsReached.get()) {
                        // 未连接且未达到最大重连次数，尝试重连
                        attemptReconnect();
                        lastHeartbeat = currentTime; // 重置心跳时间
                        lastConnectionCheck = currentTime;
                    } else if (currentlyConnected) {
                        // 连接正常，等待MQTT标准心跳机制
                        // 每30秒检查一次连接状态
                        if (currentTime - lastConnectionCheck > 30000) {
                            System.out.println("MQTT连接正常，等待30秒后再次检查");
                            lastConnectionCheck = currentTime;
                        }
                        
                        Thread.sleep(10000); // 每10秒检查一次
                    } else {
                        // 已达到最大重连次数，等待更长时间再尝试
                        System.out.println("已达到最大重连次数，等待60秒后重新尝试...");
                        Thread.sleep(60000);
                        // 重置重连计数，重新尝试
                        reconnectAttempts.set(0);
                        maxAttemptsReached.set(false);
                        lastHeartbeat = System.currentTimeMillis();
                        lastConnectionCheck = System.currentTimeMillis();
                        System.out.println("重置重连计数，重新开始连接尝试");
                    }
                } catch (InterruptedException e) {
                    if (!running.get()) {
                        break; // 正常停止
                    }
                    System.err.println("监听线程被中断: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("监听线程异常: " + e.getMessage());
                    try {
                        Thread.sleep(5000); // 异常后等待5秒再继续
                    } catch (InterruptedException ie) {
                        if (!running.get()) break;
                    }
                }
            }
        });
        listeningThread.setDaemon(true);
        listeningThread.setName("MQTT-Listener");
        listeningThread.start();
    }

    private void attemptReconnect() {
        if (!running.get() || maxAttemptsReached.get()) {
            return;
        }

        // 在重连前先检查是否已经连接
        if (isConnected()) {
            System.out.println("检测到已连接，无需重连");
            connected.set(true);
            reconnectAttempts.set(0);
            maxAttemptsReached.set(false);
            return;
        }

        int attempts = reconnectAttempts.get();
        if (attempts >= MAX_RECONNECT_ATTEMPTS) {
            System.err.println("达到最大重连次数(" + MAX_RECONNECT_ATTEMPTS + ")，停止重连");
            maxAttemptsReached.set(true);
            return;
        }

        // 计算重连延迟时间（指数退避）
        int delay = Math.min(INITIAL_RECONNECT_DELAY * (1 << attempts), MAX_RECONNECT_DELAY);
        
        System.out.println("尝试重连... (第" + (attempts + 1) + "次，延迟" + delay + "ms)");
        
        try {
            Thread.sleep(delay);
            
            if (!running.get()) {
                return;
            }

            // 再次检查连接状态，避免重复连接
            if (isConnected()) {
                System.out.println("重连前检测到已连接，跳过重连");
                connected.set(true);
                reconnectAttempts.set(0);
                maxAttemptsReached.set(false);
                return;
            }

            // 尝试连接
            mqttClient.connect(connOpts);
            
            if (isConnected()) {
                System.out.println("重连成功！");
                connected.set(true);
                reconnectAttempts.set(0); // 重置重连计数
                maxAttemptsReached.set(false); // 重置最大尝试标志
                
                // 重新订阅之前的主题
                resubscribeTopics();
                
                // 通知重连成功，让GroupManagerRunner重新订阅主题
                if (onReconnectSuccessCallback != null) {
                    try {
                        onReconnectSuccessCallback.run();
                    } catch (Exception e) {
                        System.err.println("执行重连成功回调时出错: " + e.getMessage());
                    }
                }
            } else {
                throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
            }
            
        } catch (InterruptedException e) {
            if (!running.get()) {
                return; // 正常停止
            }
            System.err.println("重连等待被中断");
        } catch (MqttException e) {
            // 检查是否是"已连接客户机"的情况
            if (e.getReasonCode() == 32100) { // MqttException.REASON_CODE_CLIENT_ALREADY_CONNECTED
                System.out.println("检测到客户端已连接，更新连接状态");
                connected.set(true);
                reconnectAttempts.set(0); // 重置重连计数
                maxAttemptsReached.set(false); // 重置最大尝试标志
                
                // 重新订阅之前的主题
                resubscribeTopics();
                
                // 通知重连成功，让GroupManagerRunner重新订阅主题
                if (onReconnectSuccessCallback != null) {
                    try {
                        onReconnectSuccessCallback.run();
                    } catch (Exception ex) {
                        System.err.println("执行重连成功回调时出错: " + ex.getMessage());
                    }
                }
                return;
            }
            
            // 检查是否是"已在进行连接"的情况
            if (e.getReasonCode() == MqttException.REASON_CODE_CONNECT_IN_PROGRESS) {
                System.out.println("连接正在进行中，等待连接完成...");
                // 等待一段时间让连接完成
                try {
                    Thread.sleep(5000); // 增加等待时间到5秒
                    if (isConnected()) {
                        System.out.println("连接已完成！");
                        connected.set(true);
                        reconnectAttempts.set(0);
                        maxAttemptsReached.set(false);
                        resubscribeTopics();
                        
                        // 通知重连成功，让GroupManagerRunner重新订阅主题
                        if (onReconnectSuccessCallback != null) {
                            try {
                                onReconnectSuccessCallback.run();
                            } catch (Exception ex) {
                                System.err.println("执行重连成功回调时出错: " + ex.getMessage());
                            }
                        }
                        return;
                    } else {
                        System.out.println("等待后仍未连接，继续重连流程");
                        // 不增加重连计数，因为这是正常的连接进行中状态
                        return;
                    }
                } catch (InterruptedException ie) {
                    if (!running.get()) return;
                }
            }
            
            int newAttempts = reconnectAttempts.incrementAndGet();
            System.err.println("重连失败 (第" + newAttempts + "次): " + e.getMessage());
            
            // 检查是否达到最大重连次数
            if (newAttempts >= MAX_RECONNECT_ATTEMPTS) {
                System.err.println("已达到最大重连次数(" + MAX_RECONNECT_ATTEMPTS + ")，将停止重连");
                maxAttemptsReached.set(true);
            }
        }
    }

    public void subscribeTopic(String topic) {
        if (!isConnected()) {
            System.err.println("MQTT未连接，无法订阅主题: " + topic);
            return;
        }
        
        try {
            mqttClient.subscribe(topic);
            if (!SUBSCRIBED_TOPICS.contains(topic)) {
                SUBSCRIBED_TOPICS.add(topic);
            }
            System.out.println("订阅主题成功: " + topic);
        } catch (MqttException e) {
            System.err.println("订阅主题失败: " + topic + ", 错误: " + e.getMessage());
        }
    }

    public void publishMessage(String topic, String payload) {
        if (!isConnected()) {
            System.err.println("MQTT未连接，无法发布消息: " + topic);
            return;
        }
        
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1); // 降低QoS级别，提高可靠性
            mqttClient.publish(topic, message);
            System.out.println("消息发布成功: " + topic + " -> " + payload);
        } catch (MqttException e) {
            System.err.println("发布消息失败: " + topic + ", 错误: " + e.getMessage());
        }
    }

    private void resubscribeTopics() {
        if (SUBSCRIBED_TOPICS.isEmpty()) {
            return;
        }
        
        System.out.println("重新订阅之前的主题...");
        for (String topic : SUBSCRIBED_TOPICS) {
            try {
                mqttClient.subscribe(topic);
                System.out.println("重新订阅成功: " + topic);
            } catch (MqttException e) {
                System.err.println("重新订阅失败: " + topic + ", 错误: " + e.getMessage());
            }
        }
    }

    public void closed() {
        System.out.println("正在关闭MQTT客户端...");
        running.set(false);
        connected.set(false);
        
        // 停止监听线程
        if (listeningThread != null && listeningThread.isAlive()) {
            listeningThread.interrupt();
            try {
                listeningThread.join(5000);
                System.out.println("监听线程已停止");
            } catch (InterruptedException e) {
                System.err.println("等待监听线程停止时被中断");
            }
        }
        
        // 关闭MQTT连接
        if (mqttClient != null) {
            try {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                    System.out.println("MQTT连接已断开");
                }
                mqttClient.close();
                System.out.println("MQTT客户端已关闭");
            } catch (MqttException e) {
                System.err.println("关闭MQTT客户端时出错: " + e.getMessage());
            }
        }
        
        // 清理资源
        SUBSCRIBED_TOPICS.clear();
        reconnectAttempts.set(0);
        maxAttemptsReached.set(false);
    }

    /**
     * 获取连接状态
     * @return true 如果MQTT客户端已连接且运行正常
     */
    public boolean isConnected() {
        return connected.get() && mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 获取当前重连次数
     * @return 重连尝试次数
     */
    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }

    /**
     * 获取最大重连次数
     * @return 最大重连次数
     */
    public int getMaxReconnectAttempts() {
        return MAX_RECONNECT_ATTEMPTS;
    }

    /**
     * 获取已订阅的主题列表
     * @return 主题列表的副本
     */
    public List<String> getSubscribedTopics() {
        return new ArrayList<>(SUBSCRIBED_TOPICS);
    }

    /**
     * 获取连接状态信息
     * @return 连接状态描述
     */
    public String getConnectionStatus() {
        if (!running.get()) {
            return "MQTT客户端已停止";
        }
        if (isConnected()) {
            return "MQTT客户端已连接";
        } else {
            int attempts = getReconnectAttempts();
            int maxAttempts = getMaxReconnectAttempts();
            if (maxAttemptsReached.get()) {
                return "MQTT客户端连接失败，已达到最大重连次数(" + maxAttempts + ")，等待重新尝试";
            } else {
                return "MQTT客户端未连接，重连中... (第" + attempts + "/" + maxAttempts + "次)";
            }
        }
    }

    /**
     * 手动触发重连（重置重连计数）
     */
    public void forceReconnect() {
        if (!running.get()) {
            System.err.println("MQTT客户端已停止，无法重连");
            return;
        }
        
        System.out.println("手动触发重连...");
        reconnectAttempts.set(0);
        maxAttemptsReached.set(false);
        connected.set(false);
    }

    /**
     * 检查是否已达到最大重连次数
     * @return true 如果已达到最大重连次数
     */
    public boolean isMaxAttemptsReached() {
        return maxAttemptsReached.get();
    }

    /**
     * 发送心跳消息保持连接活跃
     */
    private void sendHeartbeat() {
        // MQTT客户端会自动发送PINGREQ消息，无需手动发送
        // 这个方法保留但不执行任何操作，避免破坏现有代码
        System.out.println("MQTT标准心跳机制已启用，无需手动发送心跳");
    }

    /**
     * 发送测试消息验证连接
     */
    public void sendTestMessage() {
        if (!isConnected()) {
            System.err.println("MQTT未连接，无法发送测试消息");
            return;
        }
        
        try {
            publishMessage("test/topic", "Hello MQTT Test " + System.currentTimeMillis());
            System.out.println("测试消息发送成功");
        } catch (Exception e) {
            System.err.println("发送测试消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取详细的连接状态信息
     */
    public String getDetailedStatus() {
        StringBuilder status = new StringBuilder();
        status.append("MQTT客户端状态:\n");
        status.append("- 运行状态: ").append(running.get() ? "运行中" : "已停止").append("\n");
        status.append("- 连接状态: ").append(isConnected() ? "已连接" : "未连接").append("\n");
        status.append("- 重连次数: ").append(getReconnectAttempts()).append("/").append(getMaxReconnectAttempts()).append("\n");
        status.append("- 最大尝试标志: ").append(maxAttemptsReached.get() ? "是" : "否").append("\n");
        status.append("- 已订阅主题数: ").append(SUBSCRIBED_TOPICS.size()).append("\n");
        
        if (mqttClient != null) {
            status.append("- MQTT客户端: ").append(mqttClient.isConnected() ? "已连接" : "未连接").append("\n");
            if (mqttClient.isConnected()) {
                status.append("- 服务器地址: ").append(BROKER).append("\n");
                status.append("- 客户端ID: ").append(CLIENT_ID).append("\n");
            }
        }
        
        return status.toString();
    }

    /**
     * 检查连接质量
     */
    public boolean isConnectionHealthy() {
        if (!isConnected()) {
            return false;
        }
        
        try {
            // 检查MQTT客户端内部状态
            if (mqttClient == null || !mqttClient.isConnected()) {
                connected.set(false);
                return false;
            }
            
            // 检查连接选项
            if (connOpts == null) {
                return false;
            }
            
            // 检查客户端状态
            return mqttClient.isConnected() && connected.get();
        } catch (Exception e) {
            System.err.println("连接质量检查失败: " + e.getMessage());
            connected.set(false);
            return false;
        }
    }

    /**
     * 设置重连成功回调
     * @param callback 重连成功时执行的回调函数
     */
    public void setOnReconnectSuccessCallback(Runnable callback) {
        this.onReconnectSuccessCallback = callback;
    }

    /**
     * 检查主题是否已订阅
     * @param topic 要检查的主题
     * @return true 如果主题已订阅
     */
    public boolean isTopicSubscribed(String topic) {
        return SUBSCRIBED_TOPICS.contains(topic);
    }

    /**
     * 获取当前订阅状态信息
     * @return 订阅状态描述
     */
    public String getSubscriptionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("当前订阅状态:\n");
        status.append("- 已订阅主题数: ").append(SUBSCRIBED_TOPICS.size()).append("\n");
        if (!SUBSCRIBED_TOPICS.isEmpty()) {
            status.append("- 已订阅主题: ").append(String.join(", ", SUBSCRIBED_TOPICS)).append("\n");
        }
        status.append("- 连接状态: ").append(isConnected() ? "已连接" : "未连接").append("\n");
        return status.toString();
    }
}