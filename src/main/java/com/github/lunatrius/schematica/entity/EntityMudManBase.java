// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica.entity;

import net.minecraft.DamageSource;
import net.minecraft.Entity;
import net.minecraft.EntityGolem;
import net.minecraft.EntityPlayer;
import net.minecraft.SharedMonsterAttributes;
import net.minecraft.World;
import java.util.List;

public abstract class EntityMudManBase extends EntityGolem {
    protected EntityMudManBase(World world) {
        super(world);
        this.setSize(0.6F, 1.8F);
        this.setHealth(this.getMaxHealth());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.setEntityAttribute(SharedMonsterAttributes.maxHealth, this.getConfiguredMaxHealth());
        this.setEntityAttribute(SharedMonsterAttributes.attackDamage, 0.0D);
        this.setEntityAttribute(SharedMonsterAttributes.followRange, 0.0D);
        this.setEntityAttribute(SharedMonsterAttributes.movementSpeed, 0.0D);
    }

    @Override
    protected boolean isAIEnabled() {
        return false;
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return 1;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        if (this.worldObj == null || this.boundingBox == null) {
            return;
        }

        List nearby = this.worldObj.getEntitiesWithinAABB(Entity.class, this.boundingBox.expand(1.25D, 1.25D, 1.25D));
        if (nearby == null || nearby.isEmpty()) {
            return;
        }

        for (Object object : nearby) {
            if (!(object instanceof Entity)) {
                continue;
            }

            Entity entity = (Entity) object;
            String simpleName = entity.getClass().getSimpleName();
            if ("EntityXPOrb".equals(simpleName) || "EntityExperienceOrb".equals(simpleName)) {
                this.worldObj.removeEntity(entity);
            }
        }
    }

    protected int getExperiencePoints(EntityPlayer player) {
        return 0;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        this.motionX = 0.0D;
        this.motionZ = 0.0D;
    }

    public void knockBack(Entity sourceEntity, float strength, double ratioX, double ratioZ) {
    }

    public void addVelocity(double velocityX, double velocityY, double velocityZ) {
    }

    @Override
    protected String getLivingSound() {
        return "mob.slime.small";
    }

    @Override
    protected String getHurtSound() {
        return "mob.slime.small";
    }

    @Override
    protected String getDeathSound() {
        return "mob.slime.small";
    }

    protected abstract double getConfiguredMaxHealth();

}
