package se.voipbusiness.core;

import com.eclipsesource.json.JsonObject;
import org.java_websocket.WebSocket;
import se.voipbusiness.core.ping.MonitorPingTimer;

import java.util.Date;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * Created by espinraf on 26/05/15.
 */
public class Monitor {
    public MonitorWebSocket wsServer = null;
    public MonitorUDPServer udpServer = null;
    public MonitorDB mdb = null;
    public MonitorPing mp = null;

    public void init(MonitorWebSocket ws, MonitorUDPServer udp, MonitorDB db){
        this.wsServer = ws;
        this.udpServer = udp;
        this.mdb = db;
    }

    public void routeToWsServer(String data){
        String updateData = mdb.updateApp(data);
        System.out.println("Updated JSON: " + updateData);
        wsServer.sendToAll(updateData);
    }

    public void updateWebpage(WebSocket con){
        String data ="";
        ConcurrentNavigableMap map = mdb.getMonitorMap();
        for(Object k   : map.keySet()){
            JsonObject jo = (JsonObject)map.get(k);
            data = jo.toString();
            wsServer.updateWebPage(con, data );
        }

    }

    public void updateWebpage(){
        String data ="";
        ConcurrentNavigableMap map = mdb.getMonitorMap();
        for(Object k   : map.keySet()){
            JsonObject jo = (JsonObject)map.get(k);
            data = jo.toString();
            wsServer.sendToAll(data);
        }

    }

    public void logMsg(String msg) {
        String jm = "{\"Id\" : \"LOG\", \"Name\" : \"LOG\", \"Type\" : \"LOG\", \"Status\" : \"" + msg + "\"}";
        wsServer.sendToAll(jm);
    }

    public void routeToUdpServer(String data){
        System.out.println("routeToUdpServer: " + data);
    }

    public void routeToMonitorDBFromCron(String ttl){
        Date d = new Date();
        if(!ttl.equals("1h")) {
            logMsg("Reseting counters: " + ttl + " Time: " + d.toString());
        }
        mdb.resetCounters(ttl);
        if(!ttl.equals("1h")) {
            logMsg("Updating counters: " + ttl + " Time: " + d.toString());
        }
        updateWebpage();
    }

    public void routeToMonitorPing(String appId, String appName, long ttw){
        mp.pingReceived(appId, appName, ttw);
    }

    public void sendAppDown(String appId){
        MonitorPingTimer mpt = (MonitorPingTimer) mp.apps.get(appId);
        mpt.timer.cancel();
        mp.removeApp(appId);
        wsServer.sendToAll("{\"AppId\" : \""  + mpt.appId + "\", \"AppName\" : \"" + mpt.appName +  "\", \"Status\" : \"NOK\" }");
        logMsg("Application DOWN: " + mpt.appId + " - " + mpt.appName);
    }

}
