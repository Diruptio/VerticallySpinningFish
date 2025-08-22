package diruptio.verticallyspinningfish.api;

public enum Status {
    OFFLINE(false),
    ONLINE(true),
    AVAILABLE(true),
    UNAVAILABLE(true);

    private final boolean online;

    Status(boolean online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isOffline() {
        return !online;
    }
}
