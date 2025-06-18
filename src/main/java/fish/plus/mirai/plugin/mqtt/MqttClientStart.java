package fish.plus.mirai.plugin.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.ArrayList;
import java.util.List;

public class MqttClientStart {
    private static final String BROKER = "tcp://127.0.0.1:1883"; // 替换为你的MQTT代理地址 [1]
    private static final String CLIENT_ID = "JavaMqttClient"; // 客户端ID [1]
    private static final List<String> SUBSCRIBED_TOPICS = new ArrayList<>();
    private static MqttClientStart instance;
    private MqttClient mqttClient;
    private volatile boolean running = true; // 添加停止标志
    private Thread listeningThread; // 保存线程引用

    private MqttClientStart() {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            mqttClient = new MqttClient(BROKER, CLIENT_ID, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(false); // 设置不清空会话以保留订阅状态 [1]
            connOpts.setAutomaticReconnect(true); // 启用自动重连功能 [3]
            connOpts.setKeepAliveInterval(60); // 设置心跳间隔为60秒 [5]

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connection lost, attempting to reconnect...");
                    int retryCount = 0;
                    while (!mqttClient.isConnected() && running) { // 添加running检查
                        try {
                            int delayMs = (1 << retryCount) * 1000; // 指数退避算法 [4]
                            if (delayMs > 60000) delayMs = 60000; // 最大延迟时间限制为60秒
                            Thread.sleep(delayMs);
                            if (running) { // 再次检查running状态
                                mqttClient.connect(connOpts);
                                System.out.println("Reconnected successfully.");
                                resubscribeTopics(); // 重连成功后恢复订阅 [1]
                            }
                        } catch (Exception e) {
                            retryCount++;
                            System.out.println("Reconnect attempt " + retryCount + " failed, retrying...");
                        }
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("Message arrived: " + topic + " " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Delivery complete");
                }
            });

            mqttClient.connect(connOpts);
            System.out.println("Connected");

            startListeningThread(); // 启动后台监听线程
        } catch (MqttException  e) {
            e.printStackTrace();
        }
    }

    public static synchronized MqttClientStart getInstance() {
        if (instance == null) {
            instance = new MqttClientStart();
        }
        return instance;
    }

    public void subscribeTopic(String topic) {
        try {
            mqttClient.subscribe(topic);
            SUBSCRIBED_TOPICS.add(topic);
            System.out.println("Subscribed to topic: " + topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publishMessage(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(2); // 设置QoS级别 [1]
            mqttClient.publish(topic, message);
            System.out.println("Message published: " + payload);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void resubscribeTopics() throws MqttException {
        for (String t : SUBSCRIBED_TOPICS) {
            mqttClient.subscribe(t);
            System.out.println("Resubscribed to topic: " + t);
        }
    }

    public void closed(){
        running = false; // 设置停止标志
        
        // 停止监听线程
        if (listeningThread != null && listeningThread.isAlive()) {
            listeningThread.interrupt();
            try {
                listeningThread.join(5000); // 等待最多5秒
            } catch (InterruptedException e) {
                System.out.println("Interrupted while waiting for listening thread to stop");
            }
        }
        
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.close(true);
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void startListeningThread() {
        listeningThread = new Thread(() -> {
            while (running) { // 使用running标志控制循环
                if (mqttClient != null && mqttClient.isConnected()) {
                    try {
                        Thread.sleep(1000); // 每隔一秒检查一次连接状态
                    } catch (InterruptedException e) {
                        if (!running) {
                            break; // 如果是正常停止，退出循环
                        }
                        e.printStackTrace();
                    }
                } else if (running) { // 只有在running为true时才尝试重连
                    System.out.println("MQTT client is not connected. Attempting to reconnect...");
                    try {
                        mqttClient.reconnect();
                        System.out.println("Reconnected successfully.");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        listeningThread.setDaemon(true); // 设置为守护线程
        listeningThread.start();
    }
}