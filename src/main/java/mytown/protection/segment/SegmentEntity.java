package mytown.protection.segment;

import myessentials.entities.Volume;
import mytown.MyTown;
import mytown.api.container.GettersContainer;
import mytown.datasource.MyTownUniverse;
import mytown.entities.Resident;
import mytown.entities.flag.FlagType;
import mytown.protection.segment.enums.EntityType;
import mytown.util.exceptions.ConditionException;
import mytown.util.exceptions.GetterException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Segment that protects against an Entity
 */
public class SegmentEntity extends Segment {

    public final List<EntityType> types = new ArrayList<EntityType>();

    public boolean shouldExist(Entity entity) {
        if(!types.contains(EntityType.TRACKED)) {
            return true;
        }

        try {
            if (condition != null && !condition.execute(entity, getters)) {
                return true;
            }
        } catch (Exception ex) {
            if(ex instanceof GetterException || ex instanceof ConditionException) {
                MyTown.instance.LOG.error("An error occurred while checking condition for entity [DIM:{}; {}, {}, {}] of type {}", entity.dimension, entity.posX, entity.posY, entity.posZ, entity.getClass().getName());
                MyTown.instance.LOG.error(ExceptionUtils.getStackTrace(ex));
                disable();
                return true;
            } else {
                throw (RuntimeException) ex;
            }
        }

        Resident owner = getOwner(entity);
        int range = getRange(entity);
        int dim = entity.dimension;
        int x = (int) Math.floor(entity.posX);
        int y = (int) Math.floor(entity.posY);
        int z = (int) Math.floor(entity.posZ);

        if(range == 0) {
            if (!hasPermissionAtLocation(owner, dim, x, y, z)) {
                return false;
            }
        } else {
            Volume rangeBox = new Volume(x-range, y-range, z-range, x+range, y+range, z+range);
            if (!hasPermissionAtLocation(owner, dim, rangeBox)) {
                return false;
            }
        }
        return true;
    }

    public boolean shouldInteract(Entity entity, Resident res) {
        if(!types.contains(EntityType.PROTECT)) {
            return true;
        }

        try {
            if (condition != null && !condition.execute(entity, getters)) {
                return true;
            }
        } catch (ConditionException ex) {
            MyTown.instance.LOG.error("An error occurred while checking condition for entity interaction by {} at [DIM:{}; {}, {}, {}] of type {}", res.getPlayerName(), entity.dimension, entity.posX, entity.posY, entity.posZ, entity.getClass().getName());
            MyTown.instance.LOG.error(ExceptionUtils.getStackTrace(ex));
            disable();
            return true;
        }

        int dim = entity.dimension;
        int x = (int) Math.floor(entity.posX);
        int y = (int) Math.floor(entity.posY);
        int z = (int) Math.floor(entity.posZ);

        if (!hasPermissionAtLocation(res, dim, x, y, z)) {
            return false;
        }

        return true;
    }

    public boolean shouldAttack(Entity entity, Resident res) {
        if(!types.contains(EntityType.PVP)) {
            return true;
        }

        try {
            if (condition != null && !condition.execute(entity, getters)) {
                return true;
            }
        } catch (ConditionException ex) {
            MyTown.instance.LOG.error("An error occurred while checking condition for entity [DIM:{}; {}, {}, {}] of type {}", entity.dimension, entity.posX, entity.posY, entity.posZ, entity.getClass().getName());
            MyTown.instance.LOG.error(ExceptionUtils.getStackTrace(ex));
            disable();
            return true;
        }

        Resident owner = getOwner(entity);
        EntityPlayer attackedPlayer = res.getPlayer();
        int dim = attackedPlayer.dimension;
        int x = (int) Math.floor(attackedPlayer.posX);
        int y = (int) Math.floor(attackedPlayer.posY);
        int z = (int) Math.floor(attackedPlayer.posZ);

        if (!hasPermissionAtLocation(owner, dim, x, y, z)) {
            return false;
        }

        return true;
    }
}
