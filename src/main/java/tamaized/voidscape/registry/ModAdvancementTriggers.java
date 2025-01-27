package tamaized.voidscape.registry;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraftforge.eventbus.api.IEventBus;
import tamaized.regutil.RegistryClass;
import tamaized.voidscape.advancement.*;

public class ModAdvancementTriggers implements RegistryClass {

	public static final EtherealEssenceTrigger ETHEREAL_ESSENCE_TRIGGER = CriteriaTriggers.register(new EtherealEssenceTrigger());
	public static final ItemUsedOnNullServantTrigger ITEM_USED_ON_NULL_SERVANT_TRIGGER = CriteriaTriggers.register(new ItemUsedOnNullServantTrigger());
	public static final ActivatePortalTrigger ACTIVATE_PORTAL_TRIGGER = CriteriaTriggers.register(new ActivatePortalTrigger());
	public static final InfusedTrigger INFUSED_TRIGGER = CriteriaTriggers.register(new InfusedTrigger());
	public static final HoeBonemealTrigger HOE_BONEMEAL_TRIGGER = CriteriaTriggers.register(new HoeBonemealTrigger());
	public static final LiquifierTrigger LIQUIFIER_TRIGGER = CriteriaTriggers.register(new LiquifierTrigger());
	public static final DefuserTrigger DEFUSER_TRIGGER = CriteriaTriggers.register(new DefuserTrigger());
	public static final ThreeByThreeTrigger THREE_BY_THREE = CriteriaTriggers.register(new ThreeByThreeTrigger());

	@Override
	public void init(IEventBus bus) {

	}

}
