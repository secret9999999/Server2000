package org.bukkit.craftbukkit.entity;

import com.google.common.base.Preconditions;
import net.minecraft.server.EntityZombie;
import net.minecraft.server.EntityZombieVillager;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;

public class CraftZombie extends CraftMonster implements Zombie {

    public CraftZombie(CraftServer server, EntityZombie entity) {
        super(server, entity);
    }

    @Override
    public EntityZombie getHandle() {
        return (EntityZombie) entity;
    }

    @Override
    public String toString() {
        return "CraftZombie";
    }

    @Override
    public EntityType getType() {
        return EntityType.ZOMBIE;
    }

    @Override
    public boolean isBaby() {
        return getHandle().isBaby();
    }

    @Override
    public void setBaby(boolean flag) {
        getHandle().setBaby(flag);
    }

    @Override
    public boolean isVillager() {
        return getHandle() instanceof EntityZombieVillager;
    }

    @Override
    public void setVillager(boolean flag) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setVillagerProfession(Villager.Profession profession) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Villager.Profession getVillagerProfession() {
        return null;
    }

    @Override
    public boolean isConverting() {
        return getHandle().isDrownConverting();
    }

    @Override
    public int getConversionTime() {
        Preconditions.checkState(isConverting(), "Entity not converting");

        return getHandle().drownedConversionTime;
    }

    @Override
    public void setConversionTime(int time) {
        if (time < 0) {
            getHandle().drownedConversionTime = -1;
            getHandle().getDataWatcher().set(EntityZombie.DROWN_CONVERTING, false);
        } else {
            getHandle().startDrownedConversion(time);
        }
    }

    @Override
    public int getAge() {
        return getHandle().isBaby() ? -1 : 0;
    }

    @Override
    public void setAge(int i) {
        getHandle().setBaby(i < 0);
    }

    @Override
    public void setAgeLock(boolean b) {
    }
    // Paper start
    @Override
    public boolean isDrowning() {
        return getHandle().isDrownConverting();
    }

    @Override
    public void startDrowning(int drownedConversionTime) {
        getHandle().startDrownedConversion(drownedConversionTime);
    }

    @Override
    public void stopDrowning() {
        getHandle().stopDrowning();
    }

    @Override
    public boolean shouldBurnInDay() {
        return getHandle().shouldBurnInDay();
    }

    @Override
    public boolean isArmsRaised() {
        return getHandle().isAggressive();
    }

    @Override
    public void setArmsRaised(final boolean raised) {
        getHandle().setAggressive(raised);
    }

    @Override
    public void setShouldBurnInDay(boolean shouldBurnInDay) {
        getHandle().setShouldBurnInDay(shouldBurnInDay);
    }
    // Paper end

    @Override
    public boolean getAgeLock() {
        return false;
    }

    @Override
    public void setBaby() {
        getHandle().setBaby(true);
    }

    @Override
    public void setAdult() {
        getHandle().setBaby(false);
    }

    @Override
    public boolean isAdult() {
        return !getHandle().isBaby();
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public void setBreed(boolean b) {
    }
}
