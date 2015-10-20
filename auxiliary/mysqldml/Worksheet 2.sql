CALL sp_conceptspec('http://www.ebi.ac.uk/efo/EFO_0000001');
CALL sp_conceptspec('http://www.ifomis.org/bfo/1.1/snap#SpecificallyDependentContinuant');
CALL sp_conceptspec('http://www.ifomis.org/bfo/1.1/snap#Disposition');
CALL sp_conceptspec('http://www.ebi.ac.uk/efo/EFO_0000408');
CALL sp_conceptspec('http://www.ebi.ac.uk/efo/EFO_0000405');
CALL sp_conceptspec('http://www.ebi.ac.uk/efo/EFO_0000616');
CALL sp_conceptspec('http://www.ebi.ac.uk/efo/EFO_0002422');
CALL sp_conceptspec('http://www.ebi.ac.uk/efo/EFO_0000232');
CALL sp_conceptspec('http://www.ebi.ac.uk/efo/EFO_1000225');


SELECT f_concept_ancestors_count(3);

SELECT f_get_owlid_from_iri('http://www.ebi.ac.uk/efo/EFO_0000408');

-- All ancestors query

  SELECT h.superclass,
         n.name,
         h.distance,
         o.iri,
         o.type
    FROM hierarchy h
         INNER JOIN owl_objects o ON h.superclass = o.id
         INNER JOIN names n ON h.superclass = n.id
   WHERE h.subclass = 3
ORDER BY h.distance DESC, h.superclass;

  SELECT count(h.superclass) hopcount
    FROM hierarchy h
         INNER JOIN owl_objects o ON h.superclass = o.id
         INNER JOIN names n ON h.superclass = n.id
   WHERE h.subclass = 3 AND h.superclass <> 3
ORDER BY h.distance DESC, h.superclass;


-- All descendents query

  SELECT h.subclass,
         n.name,
         h.distance,
         o.iri,
         o.type
    FROM hierarchy h
         INNER JOIN owl_objects o ON h.subclass = o.id
         INNER JOIN names n ON h.subclass = n.id
   WHERE h.superclass = 215
ORDER BY h.distance ASC, h.subclass;

-- All leaf descendents query

  SELECT h.subclass,
         n.name,
         h.distance,
         o.iri,
         o.type
    FROM hierarchy h
         INNER JOIN owl_objects o ON h.subclass = o.id
         INNER JOIN names n ON h.subclass = n.id
         INNER JOIN leaves l ON h.subclass = l.id
   WHERE h.superclass = 215
ORDER BY h.distance ASC, h.subclass;

  SELECT h.subclass, n.name, h.distance
    FROM owltosql.hierarchy h
         INNER JOIN owltosql.owl_objects o ON h.subclass = o.id
         INNER JOIN owltosql.names n ON h.subclass = n.id
         INNER JOIN owltosql.leaves l ON h.subclass = l.id
   WHERE h.superclass =
            (SELECT w.id
               FROM owltosql.owl_objects w
              WHERE LOWER(w.iri) =
                       LOWER('http://www.ebi.ac.uk/efo/EFO_0000232')) and h.superclass <> h.subclass
ORDER BY h.distance ASC, h.subclass;


SELECT w.id
  FROM owltosql.owl_objects w
 WHERE LOWER(w.iri) = LOWER('http://www.ebi.ac.uk/efo/EFO_0000408')