USE owltosql;

-- VTO Ontology
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_0059139');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_0059078');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_0058822');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_9026326');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_0000008');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_0000007');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_9046416');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_0058702');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_9002086');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_9024152');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_9024151');
-- CALL sp_conceptspec('http://purl.obolibrary.org/obo/VTO_0000006');

-- OMIM Ontology
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU003422');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU000376');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU000224');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU000004');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU000477');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU001848');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU000139');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU000243');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU000058');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU000026');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU004756');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/OMIM/MTHU001443');

-- SNMI Ontology
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/A-13000');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/A-26000');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/A-2B000');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/C-E2000');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/C-E1000');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/C-E3000');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/M-70300');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/M-73000');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/M-74000');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/M-74400');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/M-74200');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/SNMI/M-74300');

-- ICD10CM Ontology
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/I63.34');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/I63.3');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/I60-I69');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/M43.2');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/M43');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/M91-M94');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/E00');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/E08-E13');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/E40-E46');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/F40.1');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/F40');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/F40-F48');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/O33.6');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/O33');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/O30-O48');
-- CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/R00');
CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/R03');
CALL sp_conceptspec('http://purl.bioontology.org/ontology/ICD10CM/R00-R09');


CALL sp_conceptspec('');


CALL sp_conceptspec('');



--SHOW profiles;

SELECT UNIX_TIMESTAMP();

SELECT f_get_owlid_from_iri('http://purl.bioontology.org/ontology/SNMI/A-13000');

SELECT o.id
     FROM owltosql.owl_objects o
    WHERE o.type = 'Class' AND LOWER(o.iri) = LOWER('http://purl.bioontology.org/ontology/OMIM/MTHU000376');
	
-- All leaf descendents query

  SELECT h.subclass,
         n.name,
         h.distance,
         o.iri,
         o.type
    FROM hierarchy h
         INNER JOIN owl_objects o ON h.subclass = o.id
         INNER JOIN names n ON h.subclass = n.id
   WHERE h.superclass = 72027
ORDER BY h.distance ASC, h.subclass;

  SELECT h.subclass, n.name, h.distance
    FROM owltosql.hierarchy h
         INNER JOIN owltosql.owl_objects o ON h.subclass = o.id
         INNER JOIN owltosql.names n ON h.subclass = n.id
         INNER JOIN owltosql.leaves l ON h.subclass = l.id
   WHERE     o.type = 'Class'
         AND h.superclass =
                (SELECT w.id
                   FROM owltosql.owl_objects w
                  WHERE LOWER(w.iri) =
                           LOWER(
                              'http://purl.bioontology.org/ontology/SNMI/A-13000'))
         AND h.superclass <> h.subclass
ORDER BY h.distance ASC, h.subclass;