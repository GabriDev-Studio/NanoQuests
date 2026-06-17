# NanoQuests v1.0.0

Квест-система для Minecraft 1.16.5. 30 квестов, 4 линии, GUI-меню.

## Требования
- Spigot/Paper 1.16.5
- Java 8+
- Плагин с `/kit give <player> <kit>` (EssentialsX, CMI и т.д.)

## Установка
1. Кинь `NanoQuests-1.0.0.jar` в `plugins/`
2. Рестарт сервера
3. В своём плагине китов создай киты с именами из поля `reward_kit` в `quests.yml`

## Команды

| Команда | Алиас | Описание |
|---------|-------|----------|
| `/quests` | - | Главное меню всех линий |
| `/questfarmer` | `/qfarmer` | Открыть линию Фермера |
| `/questmine` | `/qmine` | Открыть линию Шахтёра |
| `/questfish` | `/qfish` | Открыть линию Рыбака |
| `/questwood` | `/qwood` | Открыть линию Лесника |
| `/questadmin reload` | - | Перезагрузить квесты |
| `/questadmin reset <игрок>` | - | Сбросить прогресс игрока |

## Права

| Право | По умолчанию | Описание |
|-------|-------------|----------|
| `nanoquests.use` | все | Использовать команды квестов |
| `nanoquests.admin` | op | Админ-команды |

## Линии квестов (30 штук)
- **✿ Фермер** (8) - пшеница, морковь, картошка, свёкла, арбуз, хлеб, тыква, тростник
- **⛏ Шахтёр** (8) - камень, уголь, железо, золото, редстоун, лазурит, изумруд, алмаз, обсидиан
- **~ Рыбак** (7) - первый заброс → любитель → охотник → мастер → сокровища → океан → легенда
- **▲ Лесник** (7) - дуб, берёза, ель, джунгли, тёмный дуб, акация, легенда

## Добавить квест в quests.yml
```yaml
quests:
  farmer_new:
    display_name: "&a✿ Новый квест"
    description: "&7Описание"
    icon: APPLE
    category: FARMER        # FARMER | MINER | FISHER | LUMBERJACK
    objective_type: COLLECT_ITEM
    item: APPLE
    amount: 32
    reward_kit: farmer_new  # имя кита в твоём плагине
    reward_display:
      - "&e5x &fЗолотое яблоко"
```
Затем `/questadmin reload` - и квест появится в меню.

## Настройка kit_command
```yaml
# EssentialsX (по умолчанию)
kit_command: "kit give {player} {kit}"

# CMI
kit_command: "cmi kit {player} {kit}"
```

## Сборка
```
mvn clean package
```
Jar появится в `target/NanoQuests-1.0.0.jar`
"# NanoQuests" 
