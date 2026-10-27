ALTER TABLE room_types
ADD COLUMN IF NOT EXISTS name_en VARCHAR(150),
ADD COLUMN IF NOT EXISTS description_en TEXT;

ALTER TABLE extra_services
ADD COLUMN IF NOT EXISTS name_en VARCHAR(150),
ADD COLUMN IF NOT EXISTS description_en TEXT;

UPDATE room_types
SET name_en = 'Royal Ocean View Suite',
    description_en = 'Spacious suite with floor-to-ceiling oceanfront windows, natural oak finishes, and private terrace with hot tub.'
WHERE id = 1 OR name = 'Suite Royal Vista al Mar';

UPDATE room_types
SET name_en = 'Deluxe Garden Double Room',
    description_en = 'Contemporary boutique design overlooking tranquil zen gardens, acoustic soundproofing, and organic botanic amenities.'
WHERE id = 2 OR name = 'Habitación Doble Deluxe Jardín';

UPDATE room_types
SET name_en = 'Panoramic Hospentra Penthouse',
    description_en = 'The pinnacle of boutique luxury with 360-degree horizon views, separate living sanctuary, and VIP lounge privileges.'
WHERE id = 3 OR name = 'Penthouse Panorámico Hospentra';

UPDATE room_types
SET name_en = 'Serena Private Pool Villa',
    description_en = 'Secluded retreat surrounded by tropical palms, absolute privacy featuring a private heated saltwater plunge pool.'
WHERE id = 4 OR name = 'Villa Serena con Alberca Privada';

UPDATE room_types
SET name_en = 'Architectural Studio Loft',
    description_en = 'Minimalist double-height aesthetic with immersive natural daylight and bespoke walnut and polished concrete furnishings.'
WHERE id = 5 OR name = 'Studio Loft Arquitectónico';

UPDATE room_types
SET name_en = 'Grand Master Presidential Suite',
    description_en = 'Uncompromising exclusivity spanning two levels, private six-guest dining room, rooftop solarium, and curated wine cellar.'
WHERE id = 6 OR name = 'Grand Master Suite Presidencial';

UPDATE extra_services
SET name_en = 'Moët & Chandon Brut Impérial',
    description_en = 'Prestige French champagne served in an iced bucket with tempered crystal flutes.'
WHERE name LIKE 'Moët%';

UPDATE extra_services
SET name_en = 'Private Gran Reserva Red Wine',
    description_en = 'Aged oak barrel vintage with a velvety body and rich notes of dark berries.'
WHERE name LIKE 'Vino Tinto%';

UPDATE extra_services
SET name_en = 'Hospentra Signature Cocktail',
    description_en = 'Exclusive boutique mixology crafted with small-batch botanic gin and fresh citrus.'
WHERE name LIKE 'Cóctel Signature%';

UPDATE extra_services
SET name_en = 'In-Suite Relaxing Massage (60 min)',
    description_en = 'Deep relaxation manual therapy with organic essential oils in the intimacy of your suite.'
WHERE name LIKE 'Masaje Relajante%';

UPDATE extra_services
SET name_en = 'Aromatherapy & Mineral Bath Ritual',
    description_en = 'Bespoke immersion bath conditioning with pink Himalayan salts and soothing botanicals.'
WHERE name LIKE 'Ritual de Aromaterapia%';
