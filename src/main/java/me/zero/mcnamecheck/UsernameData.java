package me.zero.mcnamecheck;

/**
 * @author Brady
 * @since 10/17/2018
 */
public final class UsernameData {

    public final String username;
    public CheckStatus checkStatus;

    public UsernameData(String username) {
        this.username = username;
        this.checkStatus = CheckStatus.UNCHECKED;
    }

    public enum CheckStatus {
        AVAILABLE, UNAVAILABLE, FAILED, UNCHECKED;
    }
}
