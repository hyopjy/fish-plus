package fish.plus.mirai.plugin.manager;


import net.mamoe.mirai.console.permission.*;
import net.mamoe.mirai.console.permission.PermitteeId;



public class PermissionManager {

    private static final String PLUGIN_B_ID = "com.evolvedghost.mutegames";

    public static final PermissionId DUEL_PERMISSION = new PermissionId(PLUGIN_B_ID, "command.duel");
    public static final PermissionId ROULETTE_PERMISSION = new PermissionId(PLUGIN_B_ID, "command.roulette");


    /**
     * 授权指定用户使用决斗命令
     *
     * @param groupId 群组 ID
     * @param userId 用户 ID
     */
    public static void grantDuelPermission(long groupId, long userId, PermissionId permissionId) {
        PermitteeId permittee = new AbstractPermitteeId.ExactMember(groupId, userId);
        PermissionService.permit(permittee, permissionId);
    }

    /**
     * 撤销指定用户的决斗命令权限
     *
     * @param groupId 群组 ID
     * @param userId 用户 ID
     */
    public static void revokeDuelPermission(long groupId, long userId, PermissionId permissionId) {
        PermitteeId permittee = new AbstractPermitteeId.ExactMember(groupId, userId);
        PermissionService.cancel(permittee, permissionId, true);
    }

    /**
     * 清空所有权限
     */
//    public static void clearAllPermission() {
//        try {
//            PermitteeId p1 = new AbstractPermitteeId.ExactMember(227265762, 1811756096);
//            PermitteeId p2 = new AbstractPermitteeId.ExactMember(227265762, 952746839);
//            grantDuelPermission(227265762L, 1811756096L);
//            grantDuelPermission(227265762L, 952746839L);
//            revokeDuelPermission(227265762L, 1811756096L);
//            // 获取目标插件的所有权限
////            List<PermissionId> permissions = getPluginPermissions(PLUGIN_B_ID);
////            PermissionService service = PermissionService.getInstance();
//
//            // 遍历撤销所有权限（全局生效）
////            permissions.forEach(id ->
////                    service.cancel(
////                            PermitteeId.,  // 替代 PermissionOwner.ANYONE
////                            id,
////                            true // 强制撤销所有关联权限
////                    )
////            );
//
//        } catch (Exception e) {
//            System.err.println("清空权限失败: " + e.getMessage());
//        }
//    }

    /**
     * 获取指定插件的权限列表
     *
     * @param pluginId 插件 ID
     * @return 权限 ID 列表
     */
//    public static List<PermissionId> getPluginPermissions(String pluginId) {
//        List<PermissionId> permissions = new ArrayList<>();
//        CollectionsKt.asSequence(PermissionService.getInstance().getRegisteredPermissions()).;
//        try {
//            // 将 Kotlin Sequence 转换为 Java 可遍历的 Iterable
//            SequencesKt.asIterable(sequence).forEach(permission -> {
//                PermissionId id = permission.getId();
//                if (id != null && id.getNamespace().equals(pluginId)) {
//                    permissions.add(id);
//                }
//            });
//        } catch (Exception e) {
//            System.err.println("获取权限列表失败: " + e.getMessage());
//        }
//
//        return permissions;
//    }






    // 所有用户不授权

//     |- com.evolvedghost.mutegames:*                      The base permission
// |  |- com.evolvedghost.mutegames:command.banme       自裁指令
// |  |- com.evolvedghost.mutegames:command.blackjack   21点（Blackjack）指令
// |  |- com.evolvedghost.mutegames:command.duel        决斗指令
// |  |- com.evolvedghost.mutegames:command.mg          MuteGams管理指令
// |  |- com.evolvedghost.mutegames:command.roulette    俄罗斯轮盘指令

//    /permission cancel com.evolvedghost.mutegames:*
//
//
//
//            /permission cancel * com.evolvedghost.mutegames:*
//
//
//            /permission permittedpermissions *
//
//            /permission permit * com.evolvedghost.mutegames:*
//
//
//            /permission cancel <被许可人 ID> <权限 ID>   取消授权一个权限
///permission cancelall <被许可人 ID> <权限 ID>   取消授权一个权限及其所有子权限
///permission listpermissions    查看所有权限列表
///permission permit <被许可人 ID> <权限 ID>   授权一个权限
///permission permittedpermissions <被许可人 ID>   查看被授权权限列表
// /permission list com.evolvedghost.mutegames
}
