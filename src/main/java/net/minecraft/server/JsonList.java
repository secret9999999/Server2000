package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType; // Paper
import java.lang.reflect.Type; // Paper
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class JsonList<K, V extends JsonListEntry<K>> {

    protected static final Logger LOGGER = LogManager.getLogger();
    private static final Gson b = (new GsonBuilder()).setPrettyPrinting().create();
    private final File c;
    // Paper - replace HashMap is ConcurrentHashMap
    private final Map<String, V> d = Maps.newConcurrentMap(); private final Map<String, V> getBackingMap() { return this.d; } // Paper - OBFHELPER
    private boolean e = true;
    private static final ParameterizedType f = new ParameterizedType() {
        public Type[] getActualTypeArguments() {
            return new Type[]{JsonListEntry.class};
        }

        public Type getRawType() {
            return List.class;
        }

        public Type getOwnerType() {
            return null;
        }
    };

    public JsonList(File file) {
        this.c = file;
    }

    public File b() {
        return this.c;
    }

    public void add(V v0) {
        this.d.put(this.a(v0.getKey()), v0);

        try {
            this.save();
        } catch (IOException ioexception) {
            JsonList.LOGGER.warn("Could not save the list after adding a user.", ioexception);
        }

    }

    @Nullable
    public V get(K k0) {
        // Paper start
        // this.g();
        // return (V) this.d.get(this.a(k0)); // CraftBukkit - fix decompile error
        return (V) this.getBackingMap().computeIfPresent(this.getMappingKey(k0), (k, v) -> {
            return v.hasExpired() ? null : v;
        });
        // Paper end
    }

    public void remove(K k0) {
        this.d.remove(this.a(k0));

        try {
            this.save();
        } catch (IOException ioexception) {
            JsonList.LOGGER.warn("Could not save the list after removing a user.", ioexception);
        }

    }

    public void b(JsonListEntry<K> jsonlistentry) {
        this.remove(jsonlistentry.getKey());
    }

    public String[] getEntries() {
        return (String[]) this.d.keySet().toArray(new String[this.d.size()]);
    }

    // CraftBukkit start
    public Collection<V> getValues() {
        return this.d.values();
    }
    // CraftBukkit end

    public boolean isEmpty() {
        // return this.d.size() < 1; // Paper
        return this.getBackingMap().isEmpty(); // Paper - readability is the goal. As an aside, isEmpty() uses only sumCount() and a comparison. size() uses sumCount(), casts, and boolean logic
    }

    protected final String getMappingKey(K k0) { return a(k0); } // Paper - OBFHELPER
    protected String a(K k0) {
        return k0.toString();
    }

    protected boolean d(K k0) {
        return this.d.containsKey(this.a(k0));
    }

    private void removeStaleEntries() { g(); } // Paper - OBFHELPER
    private void g() {
        /*List<K> list = Lists.newArrayList();
        Iterator iterator = this.d.values().iterator();

        while (iterator.hasNext()) {
            V v0 = (V) iterator.next(); // CraftBukkit - decompile error

            if (v0.hasExpired()) {
                list.add(v0.getKey());
            }
        }

        iterator = list.iterator();

        while (iterator.hasNext()) {
            K k0 = (K) iterator.next(); // CraftBukkit - decompile error

            this.d.remove(this.a(k0));
        }*/

        this.getBackingMap().values().removeIf(JsonListEntry::hasExpired);
        // Paper end
    }

    protected abstract JsonListEntry<K> a(JsonObject jsonobject);

    public Collection<V> d() {
        return this.d.values();
    }

    public void save() throws IOException {
        this.removeStaleEntries(); // Paper - remove expired values before saving
        JsonArray jsonarray = new JsonArray();

        this.d.values().stream().map((jsonlistentry) -> {
            JsonObject jsonobject = new JsonObject();

            jsonlistentry.getClass();
            return (JsonObject) SystemUtils.a(jsonobject, jsonlistentry::a); // CraftBukkit - decompile error
        }).forEach(jsonarray::add);
        BufferedWriter bufferedwriter = Files.newWriter(this.c, StandardCharsets.UTF_8);
        Throwable throwable = null;

        try {
            JsonList.b.toJson(jsonarray, bufferedwriter);
        } catch (Throwable throwable1) {
            throwable = throwable1;
            throw throwable1;
        } finally {
            if (bufferedwriter != null) {
                if (throwable != null) {
                    try {
                        bufferedwriter.close();
                    } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                } else {
                    bufferedwriter.close();
                }
            }

        }

    }

    public void load() throws IOException {
        if (this.c.exists()) {
            BufferedReader bufferedreader = Files.newReader(this.c, StandardCharsets.UTF_8);
            Throwable throwable = null;

            try {
                JsonArray jsonarray = (JsonArray) JsonList.b.fromJson(bufferedreader, JsonArray.class);

                this.d.clear();
                Iterator iterator = jsonarray.iterator();

                while (iterator.hasNext()) {
                    JsonElement jsonelement = (JsonElement) iterator.next();
                    JsonObject jsonobject = ChatDeserializer.m(jsonelement, "entry");
                    JsonListEntry<K> jsonlistentry = this.a(jsonobject);

                    if (jsonlistentry.getKey() != null) {
                        this.d.put(this.a(jsonlistentry.getKey()), (V) jsonlistentry); // CraftBukkit - fix decompile error
                    }
                }
            // Spigot Start
            } catch ( com.google.gson.JsonParseException ex )
            {
                org.bukkit.Bukkit.getLogger().log( java.util.logging.Level.WARNING, "Unable to read file " + this.c + ", backing it up to {0}.backup and creating new copy.", ex );
                File backup = new File( this.c + ".backup" );
                this.c.renameTo( backup );
                this.c.delete();
            // Spigot End
            } catch (Throwable throwable1) {
                throwable = throwable1;
                throw throwable1;
            } finally {
                if (bufferedreader != null) {
                    if (throwable != null) {
                        try {
                            bufferedreader.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    } else {
                        bufferedreader.close();
                    }
                }

            }

        }
    }
}
