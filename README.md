The goal of the cross language enrichment is to contextualize the target entity with meaning from different languages. 
For example, Berlin is called 베를린 in Korea, Berlijn in Dutch and Berlim in Portuguese. This helps enriching the understanding
of the entities through language perspectives.

The cross language enrichment mechanism is leveraged by DBPedia, and it is implemented into the UIMA pipeline. 
The core mechanism makes you of owl:sameAs predicate of the subject entity and extract the object.
