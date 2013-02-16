ChannelSessions.each() {    
    if(it.type.toString().equals("WEBSOCKET_REMOTE")) {
        println "Closing...";
        it.close();    
    }
}