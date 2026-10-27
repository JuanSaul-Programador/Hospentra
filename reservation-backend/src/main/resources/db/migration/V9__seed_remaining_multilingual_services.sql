UPDATE extra_services
SET name_en = 'Signature Malbec Tenderloin',
    description_en = 'Fine beef tenderloin medallion in Malbec reduction with rustic truffled puree.'
WHERE name LIKE 'Solomillo%';

UPDATE extra_services
SET name_en = 'Iberian Charcuterie & Artisan Cheeses',
    description_en = 'Selection of acorn-fed Iberian ham, cured Manchego cheese, and roasted nuts.'
WHERE name LIKE 'Tabla Ibérica%';

UPDATE extra_services
SET name_en = 'Imperial Suite Breakfast',
    description_en = 'Eggs Benedict on brioche, French toast, freshly squeezed juice, and premium bean coffee.'
WHERE name LIKE 'Desayuno Imperial%';
