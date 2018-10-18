package me.zero.mcnamecheck;

import me.zero.mcnamecheck.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Brady
 * @since 10/17/2018
 */
public final class UsernameContainer {

    private final List<UsernameData> usernames;

    UsernameContainer() {
        this.usernames = new ArrayList<>();
    }

    public final void addUsername(String name) {
        if (Util.isNameValid(name)) {
            this.usernames.add(new UsernameData(name));
        }
    }

    public final void filterByName(Predicate<String> test) {
        this.usernames.removeIf(u -> !test.test(u.username));
    }

    public final void filter(Predicate<UsernameData> test) {
        this.usernames.removeIf(u -> !test.test(u));
    }

    public final void clear() {
        this.usernames.clear();
    }

    public final int size() {
        return this.usernames.size();
    }

    public final List<UsernameData> getUsernameData() {
        return this.usernames;
    }

    public final List<String> getRawUsernames() {
        return this.usernames.stream().map(u -> u.username).collect(Collectors.toList());
    }

    public final UsernameData getByName(String name) {
        return this.usernames.stream().filter(u -> name.equalsIgnoreCase(u.username)).findFirst().orElse(null);
    }
}
