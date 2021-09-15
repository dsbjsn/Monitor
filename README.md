# MonitorUtil
跳转到其他应用，并监听使用时间

## 主要方法
checkPermission 检查权限，需要获取android.permission.PACKAGE_USAGE_STATS  
getPermission 获得权限  
openApp 打开应用，并进行监听  
retryOpenApp 获取权限返回应用时，重新尝试打开应用  
startMonitor 打开应用后，开始计时   
