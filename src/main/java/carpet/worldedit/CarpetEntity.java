package carpet.worldedit;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.metadata.EntityType;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.NullWorld;

import net.minecraft.nbt.NbtCompound;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

import static com.google.common.base.Preconditions.checkNotNull;

class CarpetEntity implements Entity {

	private final WeakReference<net.minecraft.entity.Entity> entityRef;

	CarpetEntity(net.minecraft.entity.Entity entity) {
		checkNotNull(entity);
		this.entityRef = new WeakReference<>(entity);
	}

	@Override
	public BaseEntity getState() {
		net.minecraft.entity.Entity entity = entityRef.get();
		if (entity != null) {
			String id = net.minecraft.entity.Entities.getName(entity);
			if (id != null) {
				NbtCompound tag = new NbtCompound();
				entity.writeEntityNbt(tag);
				return new BaseEntity(id, NBTConverter.fromNative(tag));
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public Location getLocation() {
		net.minecraft.entity.Entity entity = entityRef.get();
		if (entity != null) {
			Vector position = new Vector(entity.x, entity.y, entity.z);
			float yaw = entity.yaw;
			float pitch = entity.pitch;

			return new Location(CarpetAdapter.adapt(entity.world), position, yaw, pitch);
		} else {
			return new Location(NullWorld.getInstance());
		}
	}

	@Override
	public Extent getExtent() {
		net.minecraft.entity.Entity entity = entityRef.get();
		if (entity != null) {
			return CarpetAdapter.adapt(entity.world);
		} else {
			return NullWorld.getInstance();
		}
	}

	@Override
	public boolean remove() {
		net.minecraft.entity.Entity entity = entityRef.get();
		if (entity != null) {
			entity.remove();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T> T getFacet(Class<? extends T> cls) {
		net.minecraft.entity.Entity entity = entityRef.get();
		if (entity != null) {
			if (EntityType.class.isAssignableFrom(cls)) {
				return (T) new CarpetEntityType(entity);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
}
