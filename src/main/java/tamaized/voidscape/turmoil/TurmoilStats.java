package tamaized.voidscape.turmoil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.turmoil.abilities.TurmoilAbility;
import tamaized.voidscape.turmoil.abilities.TurmoilAbilityInstance;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class TurmoilStats implements SubCapability.ISubCap.ISubCapData.All {

	public static final ResourceLocation ID = new ResourceLocation(Voidscape.MODID, "turmoilstats");

	private TurmoilAbilityInstance[] slots = new TurmoilAbilityInstance[9];

	private int voidicPower = 0;
	private int nullPower = 0;
	private int insanePower = 0;
	private int spellpower = 0;
	private int rechargeRate = 0;
	private int cooldown = 0;
	private int cost = 0;
	private int crit = 0;
	private boolean voidmancerStance = false;

	private boolean dirty = false;

	@Override
	public void tick(Entity parent) {
		if (!parent.level.isClientSide() && dirty && parent instanceof ServerPlayerEntity) {
			sendToClient((ServerPlayerEntity) parent);
			dirty = false;
		}
		if (voidicPower < 1000)
			voidicPower += 1 + rechargeRate;
		if (nullPower > 0 && parent.tickCount % ((voidmancerStance ? 2 : 1) * (1 + rechargeRate)) == 0)
			nullPower--;
		if (insanePower > 0 && parent.tickCount % 10 == 0)
			insanePower--;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public CompoundNBT write(CompoundNBT nbt, Direction side) {
		return nbt;
	}

	@Override
	public void read(CompoundNBT nbt, Direction side) {

	}

	@Override
	public void write(PacketBuffer buffer) {
		for (TurmoilAbilityInstance slot : slots) {
			if (slot == null) {
				buffer.writeVarInt(-1);
				buffer.writeLong(0L);
			} else
				slot.encode(buffer);
		}
	}

	@Override
	public void read(PacketBuffer buffer) {
		Map<TurmoilAbility, TurmoilAbilityInstance> cache = new HashMap<>();
		for (int i = 0; i < 9; i++) {
			TurmoilAbilityInstance instance = TurmoilAbilityInstance.decode(buffer);
			if (instance != null) {
				if (cache.containsKey(instance.ability()))
					instance = cache.get(instance.ability());
				else
					cache.put(instance.ability(), instance);
			}
			slots[i] = instance;
		}
	}

	public int getVoidicPower() {
		return voidicPower;
	}

	public void setVoidicPower(int a) {
		voidicPower = a;
		dirty = true;
	}

	public int getNullPower() {
		return nullPower;
	}

	public void setNullPower(int a) {
		nullPower = a;
		dirty = true;
	}

	public int getInsanePower() {
		return insanePower;
	}

	public void setInsanePower(int a) {
		insanePower = a;
		dirty = true;
	}

	public int getSpellpower() {
		return spellpower;
	}

	public int getRechargeRate() {
		return rechargeRate;
	}

	public int getCooldownReduction() {
		return cooldown;
	}

	public int getCostReduction() {
		return cost;
	}

	public int getSpellCrit() {
		return crit;
	}

	public void setSlot(@Nullable TurmoilAbilityInstance ability, int slot) {
		if (slot < 0 || slot >= 9)
			return;
		slots[slot] = ability;
		dirty = true;
	}

	@Nullable
	public TurmoilAbilityInstance getAbility(int slot) {
		if (slot < 0 || slot >= 9)
			return null;
		return slots[slot];
	}

	public void executeAbility(LivingEntity caster, int slot) {
		TurmoilAbilityInstance a = getAbility(slot);
		if (a != null) {
			if (caster.level.isClientSide()) {
				a.executeClientSide(this, caster, slot);
				return;
			}
			a.execute(caster);
		}
	}

}