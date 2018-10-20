package me.zero.mcnamecheck;

/**
 * @author Brady
 * @since 10/17/2018
 */
public final class UsernameData {

    /**
     * The actual username this data container represents.
     */
    public final String username;

    /**
     * The {@link CheckStatus} of this username. Defaults to {@link CheckStatus#UNCHECKED}.
     */
    public CheckStatus checkStatus;

    /**
     * This field is only definitive if the {@link #checkStatus} is {@link CheckStatus#UNAVAILABLE}.
     */
    public boolean unmigrated;

    UsernameData(String username) {
        this.username = username;
        this.checkStatus = CheckStatus.UNCHECKED;
    }

    public enum CheckStatus {
        AVAILABLE,
        UNAVAILABLE,
        FAILED,
        UNCHECKED;
    }
}
