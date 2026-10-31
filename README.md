# BloodSteal v2 (Paper 1.21.11)

## Build
    mvn clean package
Output: target/BloodSteal.jar. Java 21 required.

## Commands
- /bwithdraw <amount>  Blood ka hissa Blood Vial item mein nikalta hai (cap = current blood).

## Gems (3x3 recipes, higher tiers need the previous gem in the centre)
| Gem | Multiplier | Ability | Recipe |
|---|---|---|---|
| Blood | 150% | Blood Burst (right-click) | RNR / NDN / RNR (R=Redstone, N=Nether Wart, D=Diamond) |
| Crimson | 250% | Crimson Rage (right-click) | IRI / RGR / IRI, centre G = Blood Gem |
| Shadow | 400% | Shadow Blink (right-click) | EOE / OCO / EOE, centre C = Crimson Gem |
| Inferno | 600% | Infernal Execution (right-click) | MBM / BSB / MBM, centre S = Shadow Gem |
| Averon | 900% | Right-click: Blood Dominion. Shift+right-click: Averon Rift. Passive: Supreme Blood | NTN / NFN / NTN, centre F = Inferno Gem |

## Custom icons (resource pack)
Gems use CustomModelData 9101 to 9105 on their base item (red dye, redstone, echo shard, blaze rod, nether star).
Icons are in icons/. Put each PNG in assets/bloodsteal/textures/item/ and add a model override.
Example, assets/minecraft/models/item/red_dye.json:
    {"parent":"item/generated","textures":{"layer0":"minecraft:item/red_dye"},
     "overrides":[{"predicate":{"custom_model_data":9101},"model":"bloodsteal/item/blood_gem"}]}
and assets/bloodsteal/models/item/blood_gem.json:
    {"parent":"item/generated","textures":{"layer0":"bloodsteal:item/blood_gem"}}

## Rules
- Blood starts at 200%. A gem in hand raises the max (MULTIPLY mode: 200 x multiplier, or SET mode in config).
- PvP kill: victim loses pvp-steal-amount (10%), killer gains what was taken. Normal deaths do nothing.
- Blood at 0 = ban (configurable).
- Team members cannot damage each other (pvp.allow-team-damage: false) and cannot steal blood from each other.
- Every multiplier, cooldown and ability value is in config.yml.
