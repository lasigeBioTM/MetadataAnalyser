CREATE DEFINER = `owltosql` @`%`
PROCEDURE `sp_conceptspec`(IN concept_iri VARCHAR(100))
   READS SQL DATA
BEGIN
   /**/
   DECLARE spec_value                         NUMERIC(6, 4);
   -- DECLARE aux								  VARCHAR(200);
   --
   DECLARE owl_obj_id                         INTEGER DEFAULT 0;
   DECLARE concept_ancestors_count            INTEGER DEFAULT 0;
   DECLARE leaf_descendents_count             INTEGER DEFAULT 0;
   DECLARE leaf_descendents_ancestors_count   INTEGER DEFAULT 0;
   DECLARE leaf_ancestors_count               INTEGER DEFAULT 0;
   DECLARE leaft_concept_delta_sum            INTEGER DEFAULT 0;

   -- Declare _val variables to read in each record from the cursor
   DECLARE subclass_val                       INTEGER;
   DECLARE name_val                           TEXT;
   DECLARE distance_val                       INTEGER;

   --
   DECLARE no_more_rows                       BOOLEAN DEFAULT FALSE;
   DECLARE loop_cntr                          INTEGER DEFAULT 0;

   DECLARE
      leaf_descendents_cursor CURSOR FOR
           SELECT h.subclass, n.name, h.distance
             FROM owltosql.hierarchy h
                  INNER JOIN owltosql.owl_objects o ON h.subclass = o.id
                  INNER JOIN owltosql.names n ON h.subclass = n.id
                  INNER JOIN owltosql.leaves l ON h.subclass = l.id
            WHERE      o.type = 'Class' AND
      h.superclass = (SELECT w.id
                                        FROM owltosql.owl_objects w
                                       WHERE LOWER(w.iri) = LOWER(concept_iri))
                  AND h.superclass <> h.subclass
         ORDER BY h.distance ASC, h.subclass;

   -- declare handlers for exceptions
   DECLARE CONTINUE HANDLER FOR NOT FOUND SET no_more_rows = TRUE;

   -- get owl object id for the given concept iri
   SELECT owltosql.f_get_owlid_from_iri(concept_iri)
     INTO owl_obj_id;

   -- check for a valid owl object
   IF owl_obj_id > 0
   THEN
      -- get all ancestors count for the given concept
      SELECT owltosql.f_concept_ancestors_count(owl_obj_id)
        INTO concept_ancestors_count;

      -- check for worst case scenario, concept is a top ontology node
      IF concept_ancestors_count > 0
      THEN
         -- get leaf descendents average calculus
         OPEN leaf_descendents_cursor;

         SELECT FOUND_ROWS()
           INTO leaf_descendents_count;

         -- check for best case scenario, concept is a leaf ontology node
         IF leaf_descendents_count > 0
         THEN
            SET leaft_concept_delta_sum = 0;                 -- SET aux = 'S';

           leaf_loop:
            LOOP
               FETCH leaf_descendents_cursor
                  INTO subclass_val, name_val, distance_val;

               -- break loop condition
               IF no_more_rows
               THEN
                  CLOSE leaf_descendents_cursor;

                  LEAVE leaf_loop;
               END IF;

               -- get ancestors count for this leaf subclass concept
               SELECT owltosql.f_concept_ancestors_count(subclass_val)
                 INTO leaf_ancestors_count;

               -- calculate leaf to concept delta distance
               SET leaft_concept_delta_sum =
                        leaft_concept_delta_sum
                      + (leaf_ancestors_count - concept_ancestors_count);
               -- SET aux = CONCAT(aux, ';', (leaf_ancestors_count - concept_ancestors_count));

               -- print out statement
               -- SELECT subclass_val, name_val, distance_val;

               -- count the number of times looped
               SET loop_cntr = loop_cntr + 1;
            END LOOP leaf_loop;

            IF leaft_concept_delta_sum > 0
            THEN
               -- calcule specification metric for the given concept
               SET spec_value =
                      (  concept_ancestors_count
                       / (  concept_ancestors_count
                          + (leaft_concept_delta_sum / loop_cntr)));
            ELSE
               -- something went wrong in delta calculus
               SET spec_value = 0;

               SELECT spec_value;
            END IF;

            -- SELECT aux;
            SELECT concept_ancestors_count,
                   leaf_ancestors_count,
                   leaft_concept_delta_sum,
                   loop_cntr,
                   spec_value,
                   (leaft_concept_delta_sum / loop_cntr);
         ELSE
            CLOSE leaf_descendents_cursor;

            -- this is a leaf concept, set the highest spec value
            SET spec_value = 1;

            SELECT spec_value;
         END IF;
      ELSE
         -- this is a top concept in the ontology hierarchy
         SET spec_value = 0;

         SELECT spec_value;
      END IF;
   ELSE
      -- this is not a valid concept, set the least spec value
      SET spec_value = 0;

      SELECT spec_value;
   END IF;
END