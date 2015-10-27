CREATE DEFINER = `owltosql` @`%`
FUNCTION `f_concept_ancestors_count`(owl_obj_id INTEGER(10))
   RETURNS int(11)
BEGIN
   DECLARE result_value   INTEGER DEFAULT 0;

     -- get all ancestors count for the given concept
     SELECT count(h.superclass) hopcount
       INTO result_value
       FROM owltosql.hierarchy h
            INNER JOIN owltosql.owl_objects o ON h.superclass = o.id
            INNER JOIN owltosql.names n ON h.superclass = n.id
      WHERE     h.subclass = owl_obj_id
            AND h.superclass <> owl_obj_id
            AND o.type = 'Class'
   ORDER BY h.distance DESC, h.superclass;

   RETURN (result_value);
END