package net.minecraft.server;

// CraftBukkit start
import java.net.InetAddress;
import java.util.HashMap;
// CraftBukkit end

public class HandshakeListener implements PacketHandshakingInListener {

    private static final com.google.gson.Gson gson = new com.google.gson.Gson(); // Spigot
    // CraftBukkit start - add fields
    private static final HashMap<InetAddress, Long> throttleTracker = new HashMap<InetAddress, Long>();
    private static int throttleCounter = 0;
    // CraftBukkit end
    private static final IChatBaseComponent a = new ChatComponentText("Ignoring status request");
    private final MinecraftServer b;
    private final NetworkManager c; final NetworkManager getNetworkManager() { return this.c; } // Paper - OBFHELPER

    public HandshakeListener(MinecraftServer minecraftserver, NetworkManager networkmanager) {
        this.b = minecraftserver;
        this.c = networkmanager;
    }

    @Override
    public void a(PacketHandshakingInSetProtocol packethandshakinginsetprotocol) {
        switch (packethandshakinginsetprotocol.b()) {
            case LOGIN:
                this.c.setProtocol(EnumProtocol.LOGIN);
                IChatBaseComponent chatmessage; // Paper - Fix hex colors not working in some kick messages

                // CraftBukkit start - Connection throttle
                try {
                    long currentTime = System.currentTimeMillis();
                    long connectionThrottle = this.b.server.getConnectionThrottle();
                    InetAddress address = ((java.net.InetSocketAddress) this.c.getSocketAddress()).getAddress();

                    synchronized (throttleTracker) {
                        if (throttleTracker.containsKey(address) && !"127.0.0.1".equals(address.getHostAddress()) && currentTime - throttleTracker.get(address) < connectionThrottle) {
                            throttleTracker.put(address, currentTime);
                            chatmessage = org.bukkit.craftbukkit.util.CraftChatMessage.fromString(com.destroystokyo.paper.PaperConfig.connectionThrottleKickMessage, true)[0]; // Paper - Configurable connection throttle kick message // Paper - Fix hex colors not working in some kick messages
                            this.c.sendPacket(new PacketLoginOutDisconnect(chatmessage));
                            this.c.close(chatmessage);
                            return;
                        }

                        throttleTracker.put(address, currentTime);
                        throttleCounter++;
                        if (throttleCounter > 200) {
                            throttleCounter = 0;

                            // Cleanup stale entries
                            java.util.Iterator iter = throttleTracker.entrySet().iterator();
                            while (iter.hasNext()) {
                                java.util.Map.Entry<InetAddress, Long> entry = (java.util.Map.Entry) iter.next();
                                if (entry.getValue() > connectionThrottle) {
                                    iter.remove();
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    org.apache.logging.log4j.LogManager.getLogger().debug("Failed to check connection throttle", t);
                }
                // CraftBukkit end

                if (packethandshakinginsetprotocol.c() > SharedConstants.getGameVersion().getProtocolVersion()) {
                    chatmessage = org.bukkit.craftbukkit.util.CraftChatMessage.fromString( java.text.MessageFormat.format( org.spigotmc.SpigotConfig.outdatedServerMessage.replaceAll("'", "''"), SharedConstants.getGameVersion().getName() ) , true)[0]; // Spigot // Paper - Fix hex colors not working in some kick messages
                    this.c.sendPacket(new PacketLoginOutDisconnect(chatmessage));
                    this.c.close(chatmessage);
                } else if (packethandshakinginsetprotocol.c() < SharedConstants.getGameVersion().getProtocolVersion()) {
                    chatmessage = org.bukkit.craftbukkit.util.CraftChatMessage.fromString( java.text.MessageFormat.format( org.spigotmc.SpigotConfig.outdatedClientMessage.replaceAll("'", "''"), SharedConstants.getGameVersion().getName() ) , true)[0]; // Spigot // Paper - Fix hex colors not working in some kick messages
                    this.c.sendPacket(new PacketLoginOutDisconnect(chatmessage));
                    this.c.close(chatmessage);
                } else {
                    this.c.setPacketListener(new LoginListener(this.b, this.c));
                // Paper start - handshake event
                boolean proxyLogicEnabled = org.spigotmc.SpigotConfig.bungee;
                boolean handledByEvent = false;
                // Try and handle the handshake through the event
                if (com.destroystokyo.paper.event.player.PlayerHandshakeEvent.getHandlerList().getRegisteredListeners().length != 0) { // Hello? Can you hear me?
                    com.destroystokyo.paper.event.player.PlayerHandshakeEvent event = new com.destroystokyo.paper.event.player.PlayerHandshakeEvent(packethandshakinginsetprotocol.hostname, !proxyLogicEnabled);
                    if (event.callEvent()) {
                        // If we've failed somehow, let the client know so and go no further.
                        if (event.isFailed()) {
                            chatmessage = org.bukkit.craftbukkit.util.CraftChatMessage.fromString(event.getFailMessage(), true)[0]; // Paper - Fix hex colors not working in some kick messages
                            this.getNetworkManager().sendPacket(new PacketLoginOutDisconnect(chatmessage));
                            this.getNetworkManager().close(chatmessage);
                            return;
                        }

                        packethandshakinginsetprotocol.hostname = event.getServerHostname();
                        this.getNetworkManager().socketAddress = new java.net.InetSocketAddress(event.getSocketAddressHostname(), ((java.net.InetSocketAddress) this.getNetworkManager().getSocketAddress()).getPort());
                        this.getNetworkManager().spoofedUUID = event.getUniqueId();
                        this.getNetworkManager().spoofedProfile = gson.fromJson(event.getPropertiesJson(), com.mojang.authlib.properties.Property[].class);
                        handledByEvent = true; // Hooray, we did it!
                    }
                }
                // Don't try and handle default logic if it's been handled by the event.
                if (!handledByEvent && proxyLogicEnabled) {
                // Paper end
                    // Spigot Start
                //if (org.spigotmc.SpigotConfig.bungee) { // Paper - comment out, we check above!
                        String[] split = packethandshakinginsetprotocol.hostname.split("\00");
                        if ( split.length == 3 || split.length == 4 ) {
                            packethandshakinginsetprotocol.hostname = split[0];
                            c.socketAddress = new java.net.InetSocketAddress(split[1], ((java.net.InetSocketAddress) c.getSocketAddress()).getPort());
                            c.spoofedUUID = com.mojang.util.UUIDTypeAdapter.fromString( split[2] );
                        } else
                        {
                            chatmessage = new ChatMessage("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
                            this.c.sendPacket(new PacketLoginOutDisconnect(chatmessage));
                            this.c.close(chatmessage);
                            return;
                        }
                        if ( split.length == 4 )
                        {
                            c.spoofedProfile = gson.fromJson(split[3], com.mojang.authlib.properties.Property[].class);
                        }
                    }
                    // Spigot End
                    ((LoginListener) this.c.j()).hostname = packethandshakinginsetprotocol.hostname + ":" + packethandshakinginsetprotocol.port; // CraftBukkit - set hostname
                }
                break;
            case STATUS:
                if (this.b.al()) {
                    this.c.setProtocol(EnumProtocol.STATUS);
                    this.c.setPacketListener(new PacketStatusListener(this.b, this.c));
                } else {
                    this.c.close(HandshakeListener.a);
                }
                break;
            default:
                throw new UnsupportedOperationException("Invalid intention " + packethandshakinginsetprotocol.b());
        }

        // Paper start - NetworkClient implementation
        this.getNetworkManager().protocolVersion = packethandshakinginsetprotocol.getProtocolVersion();
        this.getNetworkManager().virtualHost = com.destroystokyo.paper.network.PaperNetworkClient.prepareVirtualHost(packethandshakinginsetprotocol.hostname, packethandshakinginsetprotocol.port);
        // Paper end
    }

    @Override
    public void a(IChatBaseComponent ichatbasecomponent) {}

    @Override
    public NetworkManager a() {
        return this.c;
    }
}
