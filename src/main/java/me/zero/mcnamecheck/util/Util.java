package me.zero.mcnamecheck.util;

import com.mojang.api.profiles.Profile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    /**
     * Finds the specified names that are not contained within the specified profile array, and returns it.
     *
     * @param names The names to check for
     * @param profiles The list of profiles potentially containing the names
     * @return The names that were not found in the profiles
     */
    public static List<String> getAbsentNames(String[] names, Profile[] profiles) {
        List<String> found = Arrays.stream(profiles)
                .map(Profile::getName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        return Arrays.stream(names)
                .map(String::toLowerCase)
                .filter(name -> !found.contains(name))
                .collect(Collectors.toList());
    }

    public static List<String> readAllLines(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            List<String> result = new ArrayList<>();
            for (;;) {
                String line = reader.readLine();
                if (line == null)
                    break;
                result.add(line);
            }
            return result;
        }
    }
}
