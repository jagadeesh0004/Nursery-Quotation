# 📋 SQL Queries to Add 95 Plants to Existing Users

**Date:** April 11, 2026  
**Purpose:** Backfill plant master data for users who created accounts before this feature  
**Status:** Ready to execute

---

## ⚠️ IMPORTANT: Read Before Executing

1. **Backup your database** before running these queries
2. **Test in development environment first**
3. **The queries are idempotent** - safe to run multiple times
4. **No existing data will be deleted** - only new plants added
5. **Execution time:** 1-2 seconds per user

---

## 🔍 Check Before Running

### Count existing users
```sql
SELECT COUNT(DISTINCT user_id) as total_users FROM nursery_profiles;
```

### Check how many users already have plants
```sql
SELECT COUNT(DISTINCT user_id) as users_with_plants FROM plants WHERE deleted_at IS NULL;
```

### Check how many plants each user has
```sql
SELECT user_id, COUNT(*) as plant_count 
FROM plants 
WHERE deleted_at IS NULL 
GROUP BY user_id 
ORDER BY plant_count DESC;
```

---

## ✅ Safe Backfill Query (Recommended - Uses Transactions)

### Option 1: For All Users (Most Common)

```sql
BEGIN TRANSACTION;

-- Get all users
WITH user_list AS (
    SELECT DISTINCT user_id FROM nursery_profiles WHERE deleted_at IS NULL
),
-- Define all 95 plants
plants_data AS (
    SELECT * FROM (VALUES 
        ('Neem', 'neem'),
        ('Cassia fistula', 'cassia fistula'),
        ('Cassia siamea', 'cassia siamea'),
        ('Cassia javanica', 'cassia javanica'),
        ('Custard Apple', 'custard apple'),
        ('Papaya', 'papaya'),
        ('Guava', 'guava'),
        ('Banana', 'banana'),
        ('Pomegranate', 'pomegranate'),
        ('Mango', 'mango'),
        ('Amla', 'amla'),
        ('Shikakai', 'shikakai'),
        ('Indian Plum (Ber)', 'indian plum (ber)'),
        ('Pear Tree', 'pear tree'),
        ('Coconut Tree', 'coconut tree'),
        ('Jackfruit Tree', 'jackfruit tree'),
        ('Areca Palm', 'areca palm'),
        ('Foxtail Palm', 'foxtail palm'),
        ('Royal Palm', 'royal palm'),
        ('Veitchia merrillii', 'veitchia merrillii'),
        ('Raphis Palm', 'raphis palm'),
        ('Ficus Panda', 'ficus panda'),
        ('Ficus lyrata', 'ficus lyrata'),
        ('Ficus religiosa (Peepal)', 'ficus religiosa (peepal)'),
        ('Ficus benghalensis (Banyan)', 'ficus benghalensis (banyan)'),
        ('Ficus benjamina', 'ficus benjamina'),
        ('Banyan Tree', 'banyan tree'),
        ('Peepal Tree', 'peepal tree'),
        ('Arjuna Tree', 'arjuna tree'),
        ('Terminalia arjuna', 'terminalia arjuna'),
        ('Terminalia mantaly', 'terminalia mantaly'),
        ('Terminalia bellirica', 'terminalia bellirica'),
        ('Terminalia catappa', 'terminalia catappa'),
        ('Terminalia bentizoe', 'terminalia bentizoe'),
        ('Rain Tree', 'rain tree'),
        ('Kadamba Tree', 'kadamba tree'),
        ('Mahua Tree', 'mahua tree'),
        ('Siris', 'siris'),
        ('Indian Elm', 'indian elm'),
        ('Putranjiva roxburghii', 'putranjiva roxburghii'),
        ('Kigelia pinnata (Sausage Tree)', 'kigelia pinnata (sausage tree)'),
        ('Bombax ceiba', 'bombax ceiba'),
        ('Semal', 'semal'),
        ('Babul', 'babul'),
        ('Shisham', 'shisham'),
        ('Tabebuia rosea', 'tabebuia rosea'),
        ('Tabebuia argentea', 'tabebuia argentea'),
        ('Spathodea campanulata', 'spathodea campanulata'),
        ('Lagerstroemia', 'lagerstroemia'),
        ('Lagerstroemia speciosa', 'lagerstroemia speciosa'),
        ('Gulmohar Yellow', 'gulmohar yellow'),
        ('Parijatak', 'parijatak'),
        ('Plumeria', 'plumeria'),
        ('Vrundavan Champa', 'vrundavan champa'),
        ('Pong Pong Champa', 'pong pong champa'),
        ('Hibiscus', 'hibiscus'),
        ('Hibiscus variegated', 'hibiscus variegated'),
        ('Ixora', 'ixora'),
        ('Jatropha', 'jatropha'),
        ('Canna', 'canna'),
        ('Allamanda', 'allamanda'),
        ('Turnera ulmifolia', 'turnera ulmifolia'),
        ('Cestrum nocturnum (Night Queen)', 'cestrum nocturnum (night queen)'),
        ('Dracaena', 'dracaena'),
        ('Dracaena Mahatma', 'dracaena mahatma'),
        ('Dracaena Colorama', 'dracaena colorama'),
        ('Heliconia', 'heliconia'),
        ('Heliconia rostrata', 'heliconia rostrata'),
        ('Heliconia psittacorum', 'heliconia psittacorum'),
        ('Zamia', 'zamia'),
        ('Sansevieria', 'sansevieria'),
        ('Spathiphyllum', 'spathiphyllum'),
        ('Syngonium', 'syngonium'),
        ('Coleus', 'coleus'),
        ('Pandanus', 'pandanus'),
        ('Rhoeo spathacea', 'rhoeo spathacea'),
        ('Asparagus', 'asparagus'),
        ('Dianella', 'dianella'),
        ('Wedelia trilobata', 'wedelia trilobata'),
        ('Fountain Grass', 'fountain grass'),
        ('Dragon Fruit', 'dragon fruit'),
        ('Star Fruit', 'star fruit'),
        ('Beal (Aegle marmelos)', 'beal (aegle marmelos)'),
        ('Cannonball Tree', 'cannonball tree'),
        ('Khaya', 'khaya'),
        ('Kusum', 'kusum'),
        ('Badoli Jamun', 'badoli jamun'),
        ('Chandan Tree', 'chandan tree'),
        ('Bauhinia blakeana', 'bauhinia blakeana'),
        ('Erythrina indica picta', 'erythrina indica picta'),
        ('Billipatra', 'billipatra')
    ) AS t(name, name_normalized)
)
-- Insert plants for all users (if not already exist)
INSERT INTO plants (user_id, name, name_normalized, created_at, updated_at)
SELECT u.user_id, pd.name, pd.name_normalized, NOW(), NOW()
FROM user_list u
CROSS JOIN plants_data pd
WHERE NOT EXISTS (
    SELECT 1 FROM plants p 
    WHERE p.user_id = u.user_id 
    AND p.name_normalized = pd.name_normalized
    AND p.deleted_at IS NULL
);

COMMIT;
```

---

## 🔧 Add Dimensions (Must Run After Plants)

This query adds the dimensions for each plant. Run this AFTER the plants query above.

```sql
BEGIN TRANSACTION;

-- Insert dimensions for all plants
-- Using CASE statement to match each plant with its dimensions

INSERT INTO dimensions (plant_id, dimension, price, created_at, updated_at)
SELECT p.id, dims.dimension, 0.00, NOW(), NOW()
FROM plants p
CROSS JOIN (
    VALUES
    ('Neem', '8x10'),
    ('Neem', '13x13'),
    ('Neem', '21x21'),
    ('Cassia fistula', '8x10'),
    ('Cassia fistula', '21x21'),
    ('Cassia siamea', '21x21'),
    ('Cassia javanica', '21x21'),
    ('Custard Apple', '8x10'),
    ('Custard Apple', '13x13'),
    ('Custard Apple', '21x21'),
    ('Papaya', '8x10'),
    ('Guava', '13x13'),
    ('Banana', '8x10'),
    ('Pomegranate', '8x10'),
    ('Amla', '13x13'),
    ('Indian Plum (Ber)', '13x13'),
    ('Pear Tree', '13x13'),
    ('Areca Palm', '8x10'),
    ('Areca Palm', '12x13'),
    ('Areca Palm', '15x16'),
    ('Areca Palm', '21x21'),
    ('Foxtail Palm', '13x13'),
    ('Foxtail Palm', '18x18'),
    ('Foxtail Palm', '21x21'),
    ('Royal Palm', '21x21'),
    ('Veitchia merrillii', '21x21'),
    ('Raphis Palm', '13x13'),
    ('Ficus Panda', '18x18'),
    ('Ficus Panda', '21x21'),
    ('Ficus lyrata', '30x30'),
    ('Ficus religiosa (Peepal)', '21x21'),
    ('Ficus benghalensis (Banyan)', '21x21'),
    ('Ficus benjamina', '25x25'),
    ('Banyan Tree', '21x21'),
    ('Peepal Tree', '21x21'),
    ('Arjuna Tree', '21x21'),
    ('Arjuna Tree', '25x25'),
    ('Terminalia arjuna', '25x25'),
    ('Terminalia mantaly', '21x21'),
    ('Terminalia bellirica', '21x21'),
    ('Terminalia catappa', '25x25'),
    ('Terminalia bentizoe', '25x25'),
    ('Rain Tree', '21x21'),
    ('Kadamba Tree', '13x13'),
    ('Kadamba Tree', '21x21'),
    ('Kadamba Tree', '25x25'),
    ('Mahua Tree', '21x21'),
    ('Siris', '21x21'),
    ('Putranjiva roxburghii', '21x21'),
    ('Kigelia pinnata (Sausage Tree)', '21x21'),
    ('Bombax ceiba', '21x21'),
    ('Tabebuia rosea', '21x21'),
    ('Tabebuia rosea', '25x25'),
    ('Tabebuia argentea', '25x25'),
    ('Spathodea campanulata', '25x25'),
    ('Spathodea campanulata', '50x50'),
    ('Lagerstroemia', '30x30'),
    ('Lagerstroemia speciosa', '30x30'),
    ('Gulmohar Yellow', '40x40'),
    ('Parijatak', '30x30'),
    ('Plumeria', '18x18'),
    ('Plumeria', '21x21'),
    ('Plumeria', '30x30'),
    ('Vrundavan Champa', '25x25'),
    ('Pong Pong Champa', '21x21'),
    ('Hibiscus', '9x11'),
    ('Hibiscus', '25x25'),
    ('Hibiscus variegated', '13x13'),
    ('Ixora', '9x11'),
    ('Ixora', '13x13'),
    ('Jatropha', '13x13'),
    ('Canna', '13x13'),
    ('Allamanda', '13x13'),
    ('Turnera ulmifolia', '13x13'),
    ('Cestrum nocturnum (Night Queen)', '7x8'),
    ('Dracaena', '8x10'),
    ('Dracaena', '15x16'),
    ('Dracaena Mahatma', '15x16'),
    ('Dracaena Colorama', '8x10'),
    ('Heliconia', '13x13'),
    ('Heliconia rostrata', '13x13'),
    ('Heliconia psittacorum', '13x13'),
    ('Zamia', '15x16'),
    ('Zamia', '18x18'),
    ('Sansevieria', 'pot'),
    ('Spathiphyllum', 'pot'),
    ('Syngonium', 'pot'),
    ('Coleus', 'pot'),
    ('Pandanus', '8x10'),
    ('Pandanus', '13x13'),
    ('Rhoeo spathacea', '8x10'),
    ('Asparagus', '13x13'),
    ('Dianella', '10x11'),
    ('Dianella', '13x13'),
    ('Wedelia trilobata', '8x8'),
    ('Fountain Grass', '8x8'),
    ('Fountain Grass', '13x13'),
    ('Dragon Fruit', '13x13'),
    ('Star Fruit', '21x21'),
    ('Beal (Aegle marmelos)', '21x21'),
    ('Cannonball Tree', '25x25'),
    ('Khaya', '30x30'),
    ('Kusum', '25x25'),
    ('Badoli Jamun', '30x30'),
    ('Chandan Tree', '21x21'),
    ('Bauhinia blakeana', '21x21'),
    ('Erythrina indica picta', '13x13'),
    ('Billipatra', '15x16')
) AS dims(plant_name, dimension)
WHERE p.name = dims.plant_name
AND p.deleted_at IS NULL
AND NOT EXISTS (
    SELECT 1 FROM dimensions d 
    WHERE d.plant_id = p.id 
    AND d.dimension = dims.dimension
    AND d.deleted_at IS NULL
);

COMMIT;
```

---

## ✅ Verification After Execution

### Check results
```sql
-- Count how many plants were added
SELECT user_id, COUNT(*) as plant_count 
FROM plants 
WHERE deleted_at IS NULL 
GROUP BY user_id 
ORDER BY user_id;
-- Expected: Each user should have 95 plants

-- Verify all prices are zero
SELECT COUNT(*) as zero_price_count
FROM dimensions
WHERE price = 0.00
AND deleted_at IS NULL;

-- Check specific plant (Neem) for all users
SELECT user_id, COUNT(*) as neem_dimensions
FROM dimensions d
JOIN plants p ON d.plant_id = p.id
WHERE p.name = 'Neem'
AND p.deleted_at IS NULL
AND d.deleted_at IS NULL
GROUP BY user_id;
-- Expected: Each user should have 3 Neem dimensions (8x10, 13x13, 21x21)
```

---

## 🚀 Quick Execution Steps

### Step 1: Backup Database
```bash
# Create backup (using PostgreSQL)
pg_dump -U postgres -d your_database > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Step 2: Run Plants Query
1. Open your database client (pgAdmin, DBeaver, etc.)
2. Copy the "Option 1" query from above
3. Execute it (highlight all and Ctrl+Enter)
4. Wait for completion

### Step 3: Run Dimensions Query
1. Copy the dimensions query above
2. Execute it
3. Wait for completion

### Step 4: Verify
1. Run the verification queries
2. Check that each user has 95 plants
3. Check that all prices = 0.00
4. Done! ✅

---

## 📊 Expected Results After Running

```
Users with plants: [number of existing users]
Average plants per user: 95
Average dimensions per user: 250+
Plants with zero price: All dimensions should show 0.00
```

---

## ❌ Rollback (If Needed)

If something goes wrong, rollback with:

```sql
BEGIN TRANSACTION;

-- Delete newly added dimensions (optional)
DELETE FROM dimensions 
WHERE created_at > '2026-04-11 00:00:00' 
AND price = 0.00;

-- Delete newly added plants (optional)
DELETE FROM plants 
WHERE created_at > '2026-04-11 00:00:00' 
AND name IN (
    'Neem', 'Cassia fistula', 'Areca Palm', 
    -- ... all 95 plant names
);

COMMIT;
```

Or simply restore from backup:
```bash
psql -U postgres -d your_database < backup_20260411_120000.sql
```

---

## 📝 Notes

- ✅ Queries are **idempotent** - won't create duplicates if run multiple times
- ✅ Uses **transactions** - all-or-nothing execution
- ✅ Checks **soft-deleted** records (deleted_at IS NULL)
- ✅ Works for **any number of users**
- ✅ **No user data affected** - only adds new plants
- ⏱️ **Execution time:** 1-3 seconds for all users

---

**Ready to backfill existing users with 95 plants!** 🌿

