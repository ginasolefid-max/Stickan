package com.ironbow.stickmanarcher.data

import com.ironbow.stickmanarcher.config.GameConfig
import kotlin.math.roundToInt

class UpgradeManager(private val saveSystem: SaveSystem) {

    fun getUpgradeLevel(type: UpgradeType): Int = saveSystem.data.upgradeLevels[type.name] ?: 0
    fun isMaxed(type: UpgradeType): Boolean = getUpgradeLevel(type) >= type.maxLevel

    fun getNextUpgradeCost(type: UpgradeType): Int {
        val lvl = getUpgradeLevel(type)
        return (type.baseCost * Math.pow(1.32, lvl.toDouble())).roundToInt()
    }

    fun purchaseUpgrade(type: UpgradeType): Boolean {
        if (isMaxed(type)) return false
        val cost = getNextUpgradeCost(type)
        if (saveSystem.data.coins < cost) return false
        saveSystem.data.coins -= cost
        saveSystem.data.upgradeLevels[type.name] = getUpgradeLevel(type) + 1
        saveSystem.save()
        return true
    }

    fun getDamageMultiplier(): Float =
        saveSystem.getSelectedBow().damageMultiplier * (1f + getUpgradeLevel(UpgradeType.DAMAGE) * 0.12f)

    fun getDrawSpeedMultiplier(): Float =
        saveSystem.getSelectedBow().drawSpeedMultiplier * (1f + getUpgradeLevel(UpgradeType.DRAW_SPEED) * 0.08f)

    fun getBonusWaveArrows(): Int = getUpgradeLevel(UpgradeType.ARROW_COUNT) * 3
    fun getCritChance(): Float = (0.05f + getUpgradeLevel(UpgradeType.CRIT_CHANCE) * 0.04f).coerceAtMost(0.65f)
    fun getMaxPlayerHp(): Float = GameConfig.BASE_PLAYER_HP + getUpgradeLevel(UpgradeType.HEALTH) * 25f
    fun getMaxBarricadeHp(): Float = GameConfig.BASE_BARRICADE_HP + getUpgradeLevel(UpgradeType.HEALTH) * 40f
    fun getBarricadeDamageReduction(): Float = (getUpgradeLevel(UpgradeType.BARRICADE_ARMOR) * 0.06f).coerceAtMost(0.60f)
    fun getTrajectoryStepCount(): Int = GameConfig.TRAJECTORY_BASE_STEPS + getUpgradeLevel(UpgradeType.EAGLE_EYE) * 3

    fun buyOrEquipBow(bow: BowType): Boolean {
        val data = saveSystem.data
        if (data.unlockedBows.contains(bow.name)) {
            data.selectedBow = bow.name
            saveSystem.save()
            return true
        }
        if (data.coins < bow.costCoins || data.gems < bow.costGems) return false
        data.coins -= bow.costCoins
        data.gems -= bow.costGems
        data.unlockedBows.add(bow.name)
        data.selectedBow = bow.name
        saveSystem.save()
        return true
    }

    fun buyArrowPack(type: ArrowType): Boolean {
        if (type == ArrowType.NORMAL || saveSystem.data.coins < type.shopPriceCoins) return false
        saveSystem.data.coins -= type.shopPriceCoins
        saveSystem.addArrowAmmo(type, type.packSize)
        return true
    }

    fun buyOrEquipSkin(skin: StickmanSkin): Boolean {
        val data = saveSystem.data
        if (data.unlockedSkins.contains(skin.name)) {
            data.selectedSkin = skin.name
            saveSystem.save()
            return true
        }
        if (data.coins < skin.costCoins || data.gems < skin.costGems) return false
        data.coins -= skin.costCoins
        data.gems -= skin.costGems
        data.unlockedSkins.add(skin.name)
        data.selectedSkin = skin.name
        saveSystem.save()
        return true
    }

    fun buyOrEquipBowSkin(skin: BowSkin): Boolean {
        val data = saveSystem.data
        if (data.unlockedBowSkins.contains(skin.name)) {
            data.selectedBowSkin = skin.name
            saveSystem.save()
            return true
        }
        if (data.coins < skin.costCoins || data.gems < skin.costGems) return false
        data.coins -= skin.costCoins
        data.gems -= skin.costGems
        data.unlockedBowSkins.add(skin.name)
        data.selectedBowSkin = skin.name
        saveSystem.save()
        return true
    }

    fun buyOrEquipTrail(trail: ArrowTrail): Boolean {
        val data = saveSystem.data
        if (data.unlockedTrails.contains(trail.name)) {
            data.selectedTrail = trail.name
            saveSystem.save()
            return true
        }
        if (data.coins < trail.costCoins || data.gems < trail.costGems) return false
        data.coins -= trail.costCoins
        data.gems -= trail.costGems
        data.unlockedTrails.add(trail.name)
        data.selectedTrail = trail.name
        saveSystem.save()
        return true
    }

    fun buyOrToggleAlly(ally: AllyType): Boolean {
        val data = saveSystem.data
        if (data.unlockedAllies.contains(ally.name)) {
            if (data.activeAllies.contains(ally.name)) data.activeAllies.remove(ally.name) else data.activeAllies.add(ally.name)
            saveSystem.save()
            return true
        }
        if (data.coins < ally.costCoins || data.gems < ally.costGems) return false
        data.coins -= ally.costCoins
        data.gems -= ally.costGems
        data.unlockedAllies.add(ally.name)
        data.activeAllies.add(ally.name)
        saveSystem.save()
        return true
    }

    fun isAllyUnlocked(ally: AllyType): Boolean = saveSystem.data.unlockedAllies.contains(ally.name)
    fun isAllyActive(ally: AllyType): Boolean = saveSystem.data.activeAllies.contains(ally.name)
}
