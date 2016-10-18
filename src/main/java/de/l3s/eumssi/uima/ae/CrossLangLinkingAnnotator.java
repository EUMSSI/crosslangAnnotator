package de.l3s.eumssi.uima.ae;

import de.l3s.eumssi.uima.ts.CrossLangDBpediaResource;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.dbpedia.spotlight.uima.types.DBpediaResource;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.uima.fit.util.JCasUtil.select;

/**
 * The annotator that enriches one DBpedia resource with other DBpedia
 * resources using cross language attributes
 * Created by tu on 20/05/16.
 */
public class CrossLangLinkingAnnotator extends JCasAnnotator_ImplBase {

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	public static final String PARAM_ENDPOINT = "endPoint";

	@ConfigurationParameter(
			name = "endPoint",
			defaultValue = {"http://dbpedia.org/sparql"},
			description = "The endpoint for Spotlight SPARQL service"
			)
	private String SPOTLIGHT_ENDPOINT = "http://dbpedia.org/sparql";

	@ConfigurationParameter(
			name = "endPoint",
			defaultValue = {"http://de.dbpedia.org/sparql"},
			description = "The endpoint for Spotlight SPARQL service"
			)
	private static String SPOTLIGHT_LOCAL_ENDPOINT = "http://dbpedia.org/sparql";

	// prefixes and suffixes of the SPARQL queries
	private final static String GET_STANDARD_RES_PREF = "SELECT ?obj WHERE {<http://dbpedia.org/resource/";
	private final static String GET_OTHER_LANG_PREF_1 = "SELECT ?obj WHERE {<";
	private final static String GET_OTHER_LANG_PREF_2 = "SELECT ?obj WHERE {<http://de.dbpedia.org/resource/";
	private final static String GET_RES_SUFF = "> owl:sameAs ?obj }";

	// pattern to recognize the cross-lang resources
	private final static Pattern CROSSLANG = Pattern.compile("http://[a-zA-Z]{2}.dbpedia.org/resource/(.*)+");

	public void process(JCas jCas) throws AnalysisEngineProcessException {

		StringBuilder queryBuilder = new StringBuilder();

		// Compute cross lang resources only once per entity
		Set<String> ents = new HashSet<>();

		// Caveat 1: Assuming the DBpedia resources are ready
		for (DBpediaResource res : select(jCas, DBpediaResource.class)) {

			// Caveat 2: Assuming the values in "text_nerl" come from Label attribute.
			String resName = res.getLabel();

			resName = resName.replaceAll(" ", "_");

			System.out.println("Name: " + resName);

			if (ents.contains(resName)) continue;
			ents.add(resName);

			String query = queryBuilder.delete(0, queryBuilder.length())
					.append(GET_STANDARD_RES_PREF)
					.append(resName)
					.append(GET_RES_SUFF).toString();

			QueryEngineHTTP qeh = null;
//			String standardUri = null;
			
			

			// First get the standard DBpedia resources
//			try  {
//				qeh = new QueryEngineHTTP(SPOTLIGHT_LOCAL_ENDPOINT, query);
//				ResultSet rs = qeh.execSelect();
//				while (rs.hasNext()) {
//					QuerySolution qs = rs.next();
//					RDFNode node = qs.get("?obj");
//					String uri = node.asResource().getURI();
//					System.out.println(uri);
//					if (uri.startsWith("http://dpedia.org/resource")) {
//						standardUri = uri;
//						break;
//					}
//				}
//			} catch (Exception e) {
//				LOG.error(e.getLocalizedMessage());
//			} finally {
//				if ((qeh) != null) qeh.close();
//			}

			// Second get the cross lang resources
//			queryBuilder = queryBuilder.delete(0, queryBuilder.length());
//			if (standardUri != null) {
//				queryBuilder = queryBuilder.append(GET_OTHER_LANG_PREF_1).append(standardUri);
//			}
//			else {
//				queryBuilder = queryBuilder.append(GET_OTHER_LANG_PREF_2).append(resName);
//			}
//			query = queryBuilder.append(GET_RES_SUFF).toString();
			
			

			// construct the cross-lang dbpedia resource
			CrossLangDBpediaResource a = new CrossLangDBpediaResource(jCas,
					res.getBegin(), res.getEnd());
			List<String> refs = new ArrayList<>();
			a.setContextualScore(res.getContextualScore());
			a.setFinalScore(res.getFinalScore());

			// Check this !
			a.setLabel(res.getLabel());
			
			a.setPercentageOfSecondRank(res.getPercentageOfSecondRank());
			a.setPriorScore(res.getPriorScore());
			a.setSupport(res.getSupport());
			a.setTypes(res.getTypes());
			a.setUri(res.getUri());
			a.addToIndexes();
			
			a.setEnref(res.getLabel());
			
			System.out.println("Query: " + query);

			try {
				qeh = new QueryEngineHTTP(SPOTLIGHT_LOCAL_ENDPOINT, query);
				ResultSet rs = qeh.execSelect();
				while (rs.hasNext()) {
					QuerySolution qs = rs.next();
					RDFNode node = qs.get("?obj");
					String uri = node.asResource().getURI();
					System.out.println("uri: " + uri);

					if (CROSSLANG.matcher(uri).matches()) {
						refs.add(node.asResource().getLocalName());
					}
				}
			} catch (Exception e) {
				LOG.error(e.getLocalizedMessage());
			} finally {
				qeh.close();
			}
			
			StringArray _refs = new StringArray(jCas, refs.size());
			
			for (int i = 0; i < refs.size(); i++) {
				_refs.set(i, refs.get(i));
			}
			
			a.setLangref(_refs);
 		}
		
	}

}
