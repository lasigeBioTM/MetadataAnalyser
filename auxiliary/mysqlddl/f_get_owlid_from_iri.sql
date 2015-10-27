CREATE DEFINER = `owltosql` @`%`
FUNCTION `f_get_owlid_from_iri`(concept_iri VARCHAR(100))
   RETURNS int(11)
BEGIN
   DECLARE result_value   INTEGER DEFAULT 0;

   SELECT o.id
     INTO result_value
     FROM owltosql.owl_objects o
    WHERE o.type = 'Class' AND LOWER(o.iri) = LOWER(concept_iri);

   RETURN result_value;
END