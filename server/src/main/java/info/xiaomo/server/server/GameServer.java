package info.xiaomo.server.server;

import info.xiaomo.gameCore.protocol.NetworkService;
import info.xiaomo.gameCore.protocol.NetworkServiceBuilder;
import info.xiaomo.server.constant.GameConst;
import info.xiaomo.server.dataConfig.ConfigDataManager;
import info.xiaomo.server.db.DataCenter;
import info.xiaomo.server.event.EventRegister;
import info.xiaomo.server.processor.LogicProcessor;
import info.xiaomo.server.processor.LoginProcessor;
import info.xiaomo.server.system.schedule.ScheduleManager;
import info.xiaomo.server.util.MsgExeTimeUtil;
import lombok.Data;

/**
 * 把今天最好的表现当作明天最新的起点．．～
 * いま 最高の表現 として 明日最新の始発．．～
 * Today the best performance  as tomorrow newest starter!
 * Created by IntelliJ IDEA.
 * <p>
 * author: xiaomo
 * github: https://github.com/xiaomoinfo
 * email : xiaomo@xiaomo.info
 * QQ    : 83387856
 * Date  : 2017/7/11 15:08
 * desc  :
 * Copyright(©) 2017 by xiaomo.
 */
@Data
public class GameServer {

    private NetworkService netWork;

    private boolean state = false;

    private MessageRouter router;

    public GameServer(ServerOption option) throws Exception {
        int bossLoopGroupCount = 4;
        int workerLoopGroupCount = Runtime.getRuntime().availableProcessors() < 8 ? 8
                : Runtime.getRuntime().availableProcessors();

        GameMessagePool pool = new GameMessagePool();

        router = new MessageRouter();
        NetworkServiceBuilder builder = new NetworkServiceBuilder();
        builder.setMessagePool(pool);
        builder.setBossLoopGroupCount(bossLoopGroupCount);
        builder.setWorkerLoopGroupCount(workerLoopGroupCount);
        builder.setPort(option.getGameServerPort());
        builder.setListener(new EventListener());
        builder.setConsumer(router);

        //登录和下线
        router.registerProcessor(GameConst.QueueId.LOGIN_LOGOUT, new LoginProcessor());
        //业务队列
        router.registerProcessor(GameConst.QueueId.LOGIC, new LogicProcessor());
        MsgExeTimeUtil.setOpen(true);

        // 创建网络服务
        netWork = builder.createService();

        // 初始化数据库
        DataCenter.init(option);

        //初始化配置文件
        ConfigDataManager.getInstance().init(option.getConfigDataPath());

        // 注册事件
        EventRegister.registerPreparedListeners();

        //开启定时任务
        ScheduleManager.getInstance().start();
    }

    public MessageRouter getRouter() {
        return this.router;
    }


    public void start() {
        netWork.start();
        if (netWork.isRunning()) {
            state = true;
        }
    }

    public boolean isOpen() {
        return state;
    }

    public void stop() {
        netWork.stop();
        state = false;
    }


}
