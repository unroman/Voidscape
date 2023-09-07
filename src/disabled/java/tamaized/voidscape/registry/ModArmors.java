package tamaized.voidscape.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import tamaized.regutil.RegUtil;
import tamaized.regutil.RegistryClass;
import tamaized.voidscape.Voidscape;
import tamaized.voidscape.client.entity.model.ModelArmorCorrupt;

import javax.annotation.Nullable;

public class ModArmors implements RegistryClass {

	static class ArmorMaterial {
		static final RegUtil.ArmorMaterial VOIDIC_CRYSTAL = new RegUtil.
				ArmorMaterial("voidic_crystal", 39, new int[]{3, 6, 8, 3}, 17, SoundEvents.ARMOR_EQUIP_DIAMOND, 2F, 0.10F, () -> Ingredient.of(ModItems.VOIDIC_CRYSTAL.get()), true, false, false);

		static final RegUtil.ArmorMaterial CORRUPT = new RegUtil.
				ArmorMaterial("corrupt", 41, new int[]{3, 6, 8, 3}, 19, SoundEvents.ARMOR_EQUIP_NETHERITE, 4F, 0.15F, () -> Ingredient.of(ModItems.TENDRIL.get()), false, true, true) {
			@Override
			@OnlyIn(Dist.CLIENT)
			@SuppressWarnings("unchecked")
			public <A extends HumanoidModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, A _default) {
				ModelArmorCorrupt<LivingEntity> model = new ModelArmorCorrupt<>(Minecraft.getInstance().getEntityModels().
						bakeLayer(armorSlot == EquipmentSlot.LEGS ? ModEntities.ModelLayerLocations.MODEL_ARMOR_INSANE_INNER : ModEntities.ModelLayerLocations.MODEL_ARMOR_INSANE_OUTER));
				model.rightfoot.visible = false;
				model.leftfoot.visible = false;
				model.bodyToLeg.visible = false;
				model.rightleg.visible = false;
				model.leftleg.visible = false;
				model.body.visible = false;
				model.rightarm.visible = false;
				model.leftarm.visible = false;
				model.head.visible = false;
				model.headoverlay.visible = false;
				switch (armorSlot) {
					case FEET:
						model.rightfoot.visible = true;
						model.leftfoot.visible = true;
						break;
					case LEGS:
						model.rightleg.visible = true;
						model.leftleg.visible = true;
						break;
					case CHEST:
						model.bodyToLeg.visible = true;
						model.body.visible = true;
						model.rightarm.visible = true;
						model.leftarm.visible = true;
						float tick = entityLiving.tickCount + Minecraft.getInstance().getDeltaFrameTime();
						float scale = 0.05F;
						float amp = 0.15F;
						float offset = 0.25F;
						model.topLeftTentacle.xRot = Mth.cos(tick * scale) * amp + offset;
						model.topLeftTentacle.yRot = Mth.sin(tick * scale + 0.2F) * amp + offset;
						model.topRightTentacle.xRot = Mth.sin(tick * scale + 0.4F) * amp + offset;
						model.topRightTentacle.yRot = Mth.cos(tick * scale + 0.6F) * amp - offset;
						model.bottomLeftTentacle.xRot = Mth.sin(tick * scale + 0.7F) * amp - offset;
						model.bottomLeftTentacle.yRot = Mth.cos(tick * scale + 0.5F) * amp + offset;
						model.bottomRightTentacle.xRot = Mth.cos(tick * scale + 0.3F) * amp - offset;
						model.bottomRightTentacle.yRot = Mth.sin(tick * scale + 0.1F) * amp - offset;
						break;
					case HEAD:
						model.head.visible = true;
						model.headoverlay.visible = true;
						break;
					default:
				}
				return (A) model;
			}

			@Override
			public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, @Nullable String type) {
				return Voidscape.MODID.concat(":textures/models/armor/corrupt" + (type == null ? "" : "_overlay") + ".png");
			}
		};
	}

	public static final RegistryObject<Item> VOIDIC_CRYSTAL_HELMET = RegUtil.ToolAndArmorHelper.
			helmet(ArmorMaterial.VOIDIC_CRYSTAL, ModItems.ItemProps.LAVA_IMMUNE.properties().get(), RegUtil.makeAttributeFactory(RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_RES, AttributeModifier.Operation.ADDITION, 1D), RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_INFUSION_RES, AttributeModifier.Operation.MULTIPLY_BASE, 0.05D)));
	public static final RegistryObject<Item> VOIDIC_CRYSTAL_CHEST = RegUtil.ToolAndArmorHelper.
			chest(ArmorMaterial.VOIDIC_CRYSTAL, ModItems.ItemProps.LAVA_IMMUNE.properties().get(), RegUtil.makeAttributeFactory(RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_RES, AttributeModifier.Operation.ADDITION, 1D), RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_INFUSION_RES, AttributeModifier.Operation.MULTIPLY_BASE, 0.05D)), (stack, tick) -> ModArmors.elytra(stack));
	public static final RegistryObject<Item> VOIDIC_CRYSTAL_LEGS = RegUtil.ToolAndArmorHelper.
			legs(ArmorMaterial.VOIDIC_CRYSTAL, ModItems.ItemProps.LAVA_IMMUNE.properties().get(), RegUtil.makeAttributeFactory(RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_RES, AttributeModifier.Operation.ADDITION, 1D), RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_INFUSION_RES, AttributeModifier.Operation.MULTIPLY_BASE, 0.05D)));
	public static final RegistryObject<Item> VOIDIC_CRYSTAL_BOOTS = RegUtil.ToolAndArmorHelper.
			boots(ArmorMaterial.VOIDIC_CRYSTAL, ModItems.ItemProps.LAVA_IMMUNE.properties().get(), RegUtil.makeAttributeFactory(RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_RES, AttributeModifier.Operation.ADDITION, 1D), RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_INFUSION_RES, AttributeModifier.Operation.MULTIPLY_BASE, 0.05D)));

	public static final RegistryObject<Item> CORRUPT_HELMET = RegUtil.ToolAndArmorHelper.
			helmet(ArmorMaterial.CORRUPT, ModItems.ItemProps.LAVA_IMMUNE.properties().get(), RegUtil.makeAttributeFactory(RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_RES, AttributeModifier.Operation.ADDITION, 2D), RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_INFUSION_RES, AttributeModifier.Operation.MULTIPLY_BASE, 0.1D), RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_VISIBILITY, AttributeModifier.Operation.MULTIPLY_BASE, 0.15D)));
	public static final RegistryObject<Item> CORRUPT_CHEST = RegUtil.ToolAndArmorHelper.
			chest(ArmorMaterial.CORRUPT, ModItems.ItemProps.LAVA_IMMUNE.properties().get(), RegUtil.makeAttributeFactory(RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_RES, AttributeModifier.Operation.ADDITION, 2D), RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_INFUSION_RES, AttributeModifier.Operation.MULTIPLY_BASE, 0.1D)), (stack, tick) -> true);
	public static final RegistryObject<Item> CORRUPT_LEGS = RegUtil.ToolAndArmorHelper.
			legs(ArmorMaterial.CORRUPT, ModItems.ItemProps.LAVA_IMMUNE.properties().get(), RegUtil.makeAttributeFactory(RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_RES, AttributeModifier.Operation.ADDITION, 2D), RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_INFUSION_RES, AttributeModifier.Operation.MULTIPLY_BASE, 0.1D)));
	public static final RegistryObject<Item> CORRUPT_BOOTS = RegUtil.ToolAndArmorHelper.
			boots(ArmorMaterial.CORRUPT, ModItems.ItemProps.LAVA_IMMUNE.properties().get(), RegUtil.makeAttributeFactory(RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_RES, AttributeModifier.Operation.ADDITION, 2D), RegUtil.
					AttributeData.make(ModAttributes.VOIDIC_INFUSION_RES, AttributeModifier.Operation.MULTIPLY_BASE, 0.1D)));

	@Override
	public void init(IEventBus bus) {

	}

	public static boolean elytra(ItemStack stack) {
		if (stack.isEmpty())
			return false;
		if (!(stack.is(VOIDIC_CRYSTAL_CHEST.get())))
			return false; // Quick fail for performance, no nbt polling needed
		CompoundTag nbt = stack.getTagElement(Voidscape.MODID);
		return nbt != null && nbt.getBoolean("elytra");
	}

}