package tamaized.voidscape.world.genlayer;

import tamaized.voidscape.registry.ModBiomes;
import tamaized.voidscape.world.VoidscapeSeededBiomeProvider;
import tamaized.voidscape.world.genlayer.legacy.AreaTransformer0;
import tamaized.voidscape.world.genlayer.legacy.Context;

public enum GenLayerVoidBiomes implements AreaTransformer0 {
	INSTANCE;

	private VoidscapeSeededBiomeProvider provider;

	GenLayerVoidBiomes() {

	}

	public GenLayerVoidBiomes setup(VoidscapeSeededBiomeProvider provider) {
		this.provider = provider;
		return this;
	}

	@Override
	public int applyPixel(Context iNoiseRandom, int x, int y) {
		return iNoiseRandom.nextRandom(4) == 0 ? getRandomBiome(iNoiseRandom) : provider.getBiomeId(ModBiomes.VOID);
	}

	private int getRandomBiome(Context random) {
		return provider.getBiomeId(VoidscapeSeededBiomeProvider.BIOMES.get(1 + random.nextRandom(VoidscapeSeededBiomeProvider.BIOMES.size() - 1)));
	}
}