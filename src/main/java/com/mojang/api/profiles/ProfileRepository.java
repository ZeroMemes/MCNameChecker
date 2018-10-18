package com.mojang.api.profiles;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ProfileRepository {

    void findProfilesByNames(String[] names, BiConsumer<String[], Profile[]> onSuccess, Consumer<Exception> onError);
}
