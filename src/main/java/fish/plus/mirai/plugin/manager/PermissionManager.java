package fish.plus.mirai.plugin.manager;


import net.mamoe.mirai.console.permission.*;


public class PermissionManager {
    // 所有用户不授权

    public static void removeAllPermission(){
       // PermissionService.getInstance().cancel();

        PermitteeId p1 = new AbstractPermitteeId.ExactMember(227265762, 1811756096);
        PermitteeId p2 = new AbstractPermitteeId.ExactMember(227265762, 952746839);

//        PermissionService.getInstance().permit(p1, permission);

    }


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

}
