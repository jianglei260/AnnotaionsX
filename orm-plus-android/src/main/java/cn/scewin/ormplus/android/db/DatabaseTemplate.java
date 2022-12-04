package cn.scewin.ormplus.android.db;

public interface DatabaseTemplate {
    int delete(String sql);

    void update();

    void execute();


}
