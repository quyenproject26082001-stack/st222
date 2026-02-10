package poster.maker.data.local.entity

import poster.maker.data.model.TemplateConfigProvider
import java.util.Random

/**
 * Data model for Poster Wanted Template item
 */
data class PosterWantedItem(
    val id: Int,
    val templateId: Int,        // 1-16
    val avatarId: Int,          // 1-25
    val name: String,
    val bounty: String,
    val nameFont: String,      // ← THÊM
    val bountyFont: String,
) {
    /**
     * Get avatar asset path for Glide
     */
    fun getAvatarPath(): String {
        return "file:///android_asset/avatar/$avatarId.png"
    }

    /**
     * Get template item path for Glide
     */
    fun getTemplatePath(): String {
        return "file:///android_asset/template/$templateId/item.png"
    }

    /**
     * Get full bounty text with prefix and suffix from template config
     * Example: bounty="30,000,000" + prefix="$" + suffix="" → "$30,000,000"
     */
    fun getFullBountyText(): String {
        val config = TemplateConfigProvider.getConfig(templateId)
        return config.bountyPrefix + bounty + config.bountySuffix
    }

    companion object {

        // ✅ THÊM: Danh sách font (COPY CHÍNH XÁC từ WantedEditorActivity.fontList)
        private val AVAILABLE_FONTS = listOf(
            "Roboto Bold",
            "Roboto Medium",
            "Roboto Regular",
            "Londrina Solid",
            "Montserrat Bold",
            "Montserrat Medium",
            "Script Elegant 1",
            "Script Elegant 2",
            "Handwriting 1",
            "Script Casual",
            "Brush Style",
            "Horror Style 1",
            "Horror Style 2",
            "Horror Style 3",
            "Horror Style 4",
            "Spooky",
            "Halloween",
            "Gothic",
            "Horror Style 5",
            "Tech 3D",
            "Creative 1",
            "Creative 2",
            "Rounded",
            "Serif Classic",
            "Signature",
        )
        // Random pirate names
        private val PIRATE_NAMES = listOf(
            "THE BUTCHER",
            "THE RIPPER",
            "THE STRANGLER",
            "THE SLASHER",
            "THE REAPER",
            "THE DESTROYER",
            "THE TERROR",
            "THE MENACE",
            "THE SAVAGE",
            "SCARFACE",
            "DEAD EYE",
            "MAD DOG",
            "WILD BEAST",
            "COLD BLOOD",
            "RAZOR BLADE",
            "BONE BREAKER",
            "SKULL CRUSHER",
            "HEAD HUNTER",
            "THE VICIOUS",
            "THE RUTHLESS",
            "THE MERCILESS",
            "THE BRUTAL",
            "THE MANIAC",
            "THE PSYCHO",
            "THE LUNATIC",
            "THE BANDIT",
            "THE OUTLAW",
            "THE FUGITIVE",
            "THE CRIMINAL",
            "THE THIEF",
            "THE SMUGGLER",
            "THE TRAITOR",
            "THE DECEIVER",
            "THE BETRAYER",
            "THE ASSASSIN",
            "THE KILLER",
            "THE MURDERER",
            "BLACK HAND",
            "RED HAND",
            "IRON CLAW",
            "VENOM FANG",
            "DEATH WISH",
            "NIGHT STALKER",
            "SHADOW KILLER",
            "DARK SLAYER",
            "BLOOD DRINKER",
            "SOUL TAKER",
            "THE TORTURER",
            "THE HANGMAN",
            "THE POISONER",
            "THE ARSONIST",
            "THE BOMBER",
            "THE SNIPER",
            "THE ENFORCER",
            "SNAKE EYES",
            "VIPER TOOTH",
            "DEMON EYES",
            "DEVIL HAND",
            "CURSE BRINGER",
            "PAIN DEALER",
            "MISERY MAKER",
            "CHAOS KING",
            "ANARCHY LORD",
            "MAYHEM MASTER",
            "THE WANTED",
            "THE HUNTED",
            "THE NOTORIOUS",
            "THE INFAMOUS",
            "THE DANGEROUS",
            "THE DEADLY",
            "THE VIOLENT",
            "THE ARMED",
            "THE HOSTILE",
            "SCAR FACE",
            "BROKEN TOOTH",
            "ONE EYE",
            "NO MERCY",
            "QUICK DRAW",
            "FAST BLADE",
            "SILENT KILLER",
            "LAST BREATH",
            "DOOMSDAY",
            "DARK TERROR",
            "FIRE BRINGER",
            "DARKSIDE",
            "BLACKOUT",
            "CROSSFIRE",
            "SURE SHOT",
            "KILL ZONE",
            "GIGGLE HOOK",
            "SCARE SAIL",
            "JOKER DEPTH",
            "TICKLE TIDE",
            "LAUGH LURK",
            "FUNNY FANG",
            "CREEPY GRIN",
            "SILLY SLASH",
            "BOO BLADE",
            "CHUCKLE CLAW",
            "SPOOKY SWAB",
            "WITTY WRAITH",
            "GOOFY GHOUL",
            "EERIE ECHO",
            "PRANK PLANK",
            "HAUNT HAH",
            "SMIRK SHADOW",
            "DROLL DOOM",
            "FREAKY FOG",
            "QUIP QUAKE",
            "BIZARRE BAY",
            "ODD OCEAN",
            "WHIMSY WRECK",
            "ZANY ZOMBIE",
            "PUN PIRATE",
            "GLEE GHOST",
            "MOCK MENACE",
            "RIDDLE RIP",
            "JEST JAWS",
            "CLOWN CURSE"
        )

        // Random bounty amounts
        private val BOUNTY_AMOUNTS = listOf(
            "30,000,000",
            "50,000,000",
            "100,000,000",
            "200,000,000",
            "300,000,000",
            "500,000,000",
            "800,000,000",
            "100,000,000",
            "150,000,000",
            "250,000,000",
            "350,000,000",
            "4,000,000",
            "550,000,000",
            "10,000,000",
            "15,000,000",
            "66,000,000",
            "77,000,000",
            "88,000,000",
            "320,000,000",
            "438,000,000",
            "30,000,000",
            "10,000,000",
            "15,000,000",
            "20,000,000",
            "40,000,000",
            "50,000,000",
            "1",
            "10",
            "100",
            "1,000",
            "10,000",
            "55,000,000",
            "666,666",
            "7,777,777",
            // Thêm giá trị từ 100M đến 1B
            "120,000,000",
            "150,000,000",
            "180,000,000",
            "220,000,000",
            "260,000,000",
            "290,000,000",
            "330,000,000",
            "370,000,000",
            "400,000,000",
            "450,000,000",
            "480,000,000",
            "520,000,000",
            "560,000,000",
            "600,000,000",
            "650,000,000",
            "700,000,000",
            "750,000,000",
            "820,000,000",
            "850,000,000",
            "900,000,000",
            "950,000,000",
            "999,999,999"
        )
        /**
         * Generate random poster wanted items
         * Uses maxNameLength and maxBountyLength from template config to limit text lengths
         */
        fun generateRandomItems(count: Int): List<PosterWantedItem> {
            val items = mutableListOf<PosterWantedItem>()
            val random = Random()

            for (i in 1..count) {
                val templateId = random.nextInt(16) + 1  // 1-16

                // Get template config to check maxBountyLength
                val config = TemplateConfigProvider.getConfig(templateId)
                val maxLength = config.maxBountyLength

                // Filter bounty amounts based on maxBountyLength
                val availableBounties = if (maxLength != null) {
                    // Limit to bounties with length <= maxLength
                    BOUNTY_AMOUNTS.filter { it.length <= maxLength }
                } else {
                    // No limit - use all bounties
                    BOUNTY_AMOUNTS
                }

                // Select random bounty from available options
                val selectedBounty = if (availableBounties.isNotEmpty()) {
                    availableBounties[random.nextInt(availableBounties.size)]
                } else {
                    // Fallback: use shortest bounty if filter is too strict
                    BOUNTY_AMOUNTS.minByOrNull { it.length } ?: BOUNTY_AMOUNTS[0]
                }

                // Filter names based on maxNameLength
                val maxNameLen = config.maxNameLength
                val availableNames = if (maxNameLen != null) {
                    // Limit to names with length <= maxNameLength
                    PIRATE_NAMES.filter { it.length <= maxNameLen }
                } else {
                    // No limit - use all names
                    PIRATE_NAMES
                }

                // Select random name from available options
                val selectedName = if (availableNames.isNotEmpty()) {
                    availableNames[random.nextInt(availableNames.size)]
                } else {
                    // Fallback: use shortest name if filter is too strict
                    PIRATE_NAMES.minByOrNull { it.length } ?: PIRATE_NAMES[0]
                }

                // ✅ THÊM: Random fonts
                val nameFont = AVAILABLE_FONTS[random.nextInt(AVAILABLE_FONTS.size)]
                val bountyFont = AVAILABLE_FONTS[random.nextInt(AVAILABLE_FONTS.size)]
                items.add(
                    PosterWantedItem(
                        id = i,
                        templateId = templateId,
                        avatarId = random.nextInt(25) + 1,        // 1-25
                        name = selectedName,  // Use filtered name
                        bounty = selectedBounty,
                        nameFont = nameFont,      // ← THÊM
                        bountyFont = bountyFont   // ← THÊM
                    )
                )
            }

            return items
        }
    }
}