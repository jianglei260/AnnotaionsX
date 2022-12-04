package cn.scewin.ormplus;

import java.util.List;

public class DaoInfo {
    private String daoInterfaceName;

    private List<DaoMethodInfo> methodInfos;

    public String getDaoInterfaceName() {
        return daoInterfaceName;
    }

    public void setDaoInterfaceName(String daoInterfaceName) {
        this.daoInterfaceName = daoInterfaceName;
    }

    public List<DaoMethodInfo> getMethodInfos() {
        return methodInfos;
    }

    public void setMethodInfos(List<DaoMethodInfo> methodInfos) {
        this.methodInfos = methodInfos;
    }
}
