  SELECT o1.id supid,
         n1.name,
         o1.type suptype,
         h.distance,
         o2.id subid,
         n2.name,
         o2.type subtype
    FROM hierarchy h
         INNER JOIN owl_objects o1 ON h.superclass = o1.id
         INNER JOIN owl_objects o2 ON h.subclass = o2.id
         INNER JOIN names n1 ON o1.id = n1.id
         INNER JOIN names n2 ON o2.id = n2.id
   WHERE o2.id = 3
ORDER BY h.distance DESC;

  SELECT o1.id supid,
         n1.name,
         o1.type suptype,
         h.distance,
         o2.id subid,
         n2.name,
         o2.type subtype
    FROM hierarchy h
         INNER JOIN owl_objects o1 ON h.superclass = o1.id
         INNER JOIN owl_objects o2 ON h.subclass = o2.id
         INNER JOIN names n1 ON o1.id = n1.id
         INNER JOIN names n2 ON o2.id = n2.id
   WHERE o1.id = 3 and h.distance = 1 
ORDER BY h.distance ASC;