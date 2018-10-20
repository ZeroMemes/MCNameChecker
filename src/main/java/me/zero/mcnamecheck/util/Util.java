package me.zero.mcnamecheck.util;

import java.util.regex.Pattern;

/**
 * @author Brady
 * @since 10/17/2018
 */
public final class Util {

    private Util() {}

    /**
     * Regex that matches valid player usernames
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    /**
     * Tests if the specified name is valid to the Minecraft username standard
     *
     * @param name The username
     * @return Whether or not the username is valid
     */
    public static boolean isNameValid(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }

    public static String capitalize(String in) {
        return in.charAt(0) + in.substring(1).toLowerCase();
    }
}
