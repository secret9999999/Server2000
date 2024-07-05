package io.papermc.generator.rewriter.types.simple;

import io.papermc.generator.rewriter.types.RegistryFieldRewriter;
import io.papermc.generator.utils.ClassHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongs;
import javax.lang.model.SourceVersion;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class JukeboxSongRewriter extends RegistryFieldRewriter<JukeboxSong> {

    private static final Map<ResourceKey<JukeboxSong>, String> FALLBACK_NAMES;

    static {
        final Map<ResourceKey<JukeboxSong>, String> map = new IdentityHashMap<>();
        try {
            for (final Field field : JukeboxSongs.class.getDeclaredFields()) {
                if (!ResourceKey.class.isAssignableFrom(field.getType())) {
                    continue;
                }

                int mod = field.getModifiers();
                if (Modifier.isPublic(mod) & Modifier.isStatic(mod) & Modifier.isFinal(mod)) {
                    if (field.getGenericType() instanceof ParameterizedType complexType && complexType.getActualTypeArguments().length == 1 &&
                        ClassHelper.eraseType(complexType.getActualTypeArguments()[0]) == JukeboxSong.class) {
                        map.put((ResourceKey<JukeboxSong>) field.get(null), field.getName());
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
        FALLBACK_NAMES = Collections.unmodifiableMap(map);
    }

    public JukeboxSongRewriter() {
        super(Registries.JUKEBOX_SONG, "get");
    }

    @Override
    protected String rewriteFieldName(Holder.Reference<JukeboxSong> reference) {
        String keyedName = super.rewriteFieldName(reference);
        if (!SourceVersion.isIdentifier(keyedName)) {
            // fallback to field names (happens for 5, 11, 13 etc.)
            return FALLBACK_NAMES.get(reference.key());
        }
        return keyedName;
    }
}
