package tamaized.voidscape.turmoil.abilities;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.network.server.ServerPacketTurmoilActivateAbility;
import tamaized.voidscape.turmoil.SubCapability;
import tamaized.voidscape.turmoil.TurmoilStats;

public final class TurmoilAbilityInstance {

	private final TurmoilAbility ability;
	private long lastCast;
	private TurmoilStats casterStats;

	public TurmoilAbilityInstance(TurmoilAbility ability) {
		this.ability = ability;
	}

	public TurmoilAbility ability() {
		return ability;
	}

	public int getCalcCost(TurmoilStats stats) {
		return (int) (ability.cost() * (1F - (float) stats.getCostReduction() / 100F));
	}

	public int getCalcCooldown(TurmoilStats stats) {
		return (int) (ability.cooldown() * (1F - (float) stats.getCooldownReduction() / 100F));
	}

	public boolean canAfford(LivingEntity caster) {
		return caster.getCapability(SubCapability.CAPABILITY).map(resolve -> resolve.get(Voidscape.subCapTurmoilStats).map(stats -> stats.getVoidicPower() >= getCalcCost(stats)).get()).orElse(false);
	}

	public boolean canExecute(LivingEntity caster) {
		return cooldownRemaining(caster.level) <= 0 && canAfford(caster);
	}

	public void executeClientSide(TurmoilStats stats, LivingEntity caster, int slot) {
		if (!canExecute(caster))
			return;
		Voidscape.NETWORK.sendToServer(new ServerPacketTurmoilActivateAbility(slot));
		stats.setVoidicPower(stats.getVoidicPower() - getCalcCost(stats));
		putOnCooldown(caster);
	}

	public void execute(LivingEntity caster) {
		if (!canExecute(caster))
			return;
		ability.execute(caster);
		caster.getCapability(SubCapability.CAPABILITY).ifPresent(cap -> cap.get(Voidscape.subCapTurmoilStats).ifPresent(stats -> {
			stats.setVoidicPower(stats.getVoidicPower() - getCalcCost(stats));
			casterStats = stats;
		}));
		putOnCooldown(caster);
	}

	private void putOnCooldown(LivingEntity caster) {
		lastCast = caster.level.getGameTime();
	}

	public long cooldownRemaining(World level) {
		return Math.max((filterCooldown() - (level.getGameTime() - lastCast)), 0);
	}

	public float cooldownPercent(World level) {
		return MathHelper.clamp((float) cooldownRemaining(level) / (float) filterCooldown(), 0F, 1F);
	}

	private int filterCooldown() {
		return casterStats == null ? ability.cooldown() : getCalcCooldown(casterStats);
	}

	public void encode(PacketBuffer packet) {
		packet.writeVarInt(ability.id());
		packet.writeLong(lastCast);
	}

	public static TurmoilAbilityInstance decode(PacketBuffer packet) {
		TurmoilAbility ability = TurmoilAbility.getFromID(packet.readVarInt());
		long data = packet.readLong();
		if (ability == null)
			return null;
		TurmoilAbilityInstance instance = new TurmoilAbilityInstance(ability);
		instance.lastCast = data;
		return instance;
	}

}