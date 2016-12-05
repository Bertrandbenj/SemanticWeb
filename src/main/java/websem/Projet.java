package websem;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.FileManager;

public class Projet {
	
	public final static String inputs = "src/main/resources/input/";
	public final static String transform = "src/main/resources/transform/";
	public final static String ttls = "src/main/resources/ttl/";
	public final static String selects = "src/main/resources/selects/";
	public final static String ask = "src/main/resources/ask/";
	
    private static final Map<String, String> toTurtle = new HashMap<String, String>();
    static {
    	toTurtle.put("etab-bts-public-men-2013-2014.csv", "btscpge.sparql");
    	toTurtle.put("etab-cpge-public-men-2013-2014.csv", "btscpge.sparql");
    	
    	toTurtle.put("etab-ensprimaire-public-men-2.csv", "primaire.sparql");
    	toTurtle.put("etab-ensseccollegial-public.csv", "college.sparql");
    	toTurtle.put("etab-enssecqualifiant-public-men.csv","lycee.sparql");
    	
    	toTurtle.put("etablissements-de-formation-des-cadres-formation-economique-juridique-administrative-et-sociale-enssup-2014.csv", "cadres.sparql");
    	toTurtle.put("etablissements-de-formation-des-cadres-formation-pedagogique-enssup-2014.csv",  "cadres.sparql");
    	toTurtle.put("etablissements-de-formation-des-cadres-formation-scientifique-et-technique-enssup-2014.csv", "cadres.sparql");
    	
    	toTurtle.put("liste-des-etablissements-prives-avril-men-2011.csv",  "prives.sparql");
//    	requestOnTTL.put(inputs+"liste-des-etablissements-publics-avril-2011-men.csv", "two");
//    	requestOnTTL.put(inputs+"universites-marocaines-enssup-2014.csv", "two");
    }
    
    
 
	static Resource school = ResourceFactory.createResource("http://schema.org/School");
	static Resource mSchool = ResourceFactory.createResource("http://schema.org/MiddleSchool");
	static Resource hSchool = ResourceFactory.createResource("http://schema.org/HighSchool");
	static Resource colOrUni = ResourceFactory.createResource("http://schema.org/CollegeOrUniversity");
	static Resource educOrg = ResourceFactory.createResource("http://schema.org/EducationalOrganization");
	
	static Property subClass = ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#subClassOf");
	static Property isA = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	static Property iClass = ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#Class");
	static Property isPublic = ResourceFactory.createProperty("http://www.data.gov.ma/data/fr/group/education/ontology.owl#isPublic");
	static Property name = ResourceFactory.createProperty("http://schema.org/name");
	static Property streetAdress = ResourceFactory.createProperty("http://schema.org/streetAddress");
	static Property telephone = ResourceFactory.createProperty("http://schema.org/telephone");
	static Property faxNumber = ResourceFactory.createProperty("http://schema.org/faxNumber");
	static Property addressLocality = ResourceFactory.createProperty("http://schema.org/addressLocality");
	static Resource literal = ResourceFactory.createResource("http://www.w3.org/2000/01/rdf1schema#Literal");
	static Resource bool = ResourceFactory.createResource("http://www.w3.org/2001/XMLSchema#boolean");
	
	
    static Model load(){
    	return toTurtle
    			.entrySet()
	    		.stream()
	    		.map(ent -> QueryExecutionFactory
			    				.create(
			    						QueryFactory.read(transform+ent.getValue()),
			    						ModelFactory.createDefaultModel().read(inputs+ent.getKey()))
			    				.execConstruct())
	    		.reduce((m1,m2)->  ModelFactory.createUnion(m1, m2))
	    		.orElse(null);
    }
    
    static void transformInputs(){

    	toTurtle.forEach((csv,trans) -> {
    		try{
    			String ttl = ttls + csv+".ttl";
    			csv = inputs+ csv;
    			
    			String cmd = "~/tarql/target/appassembler/bin/tarql "+transform+trans+" "+csv+" > "+ttl;
    			System.out.println("Exec : "+cmd);
    			Process process = new ProcessBuilder(new String[] { "bash", "-c", cmd }).start();
    			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
    			String line = null;
    			while ((line = br.readLine()) != null) {
    				System.out.println(line);
    			}
    			// There should really be a timeout here.
    			if (0 != process.waitFor())
    				System.err.println("Error processing "+cmd);
    		}catch(Exception e){
    			System.err.println("Error processing "+csv +"  "+trans);
    			e.printStackTrace();
    		}
    	});
    }
	
	

	public static void main(String[] args) throws IOException {
		System.out.println(Syntax.guessFileSyntax(transform+"req.sparql"));
		
		//transformInputs();
		
		
		QueryExecution q = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql",
						  "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
						+ "PREFIX dbo: <http://dbpedia.org/ontology/>\n" 
						+ "PREFIX db-owl: <http://dbpedia.org/ontology/>\n"
						+ "PREFIX dbpedia-fr: <http://fr.dbpedia.org/resource/>\n"
						+ "\n"
						+ "SELECT distinct ?ville ?caplat ?caplong\n" 
						+ "WHERE {\n" 
						+ "  ?ville rdf:type dbo:city.\n"
						+ "  ?ville db-owl:country dbpedia-fr:Maroc.\n" 
						+ "OPTIONAL {\n" 
						+ "    ?ville geo:lat ?caplat ;\n"
						+ "      geo:long ?caplong .\n" 
						+ "  }\n" 
						+ "}");
		
		
		
//		System.out.println(q.getQuery().toString(Syntax.defaultQuerySyntax));
//		System.out.println(ResultSetFormatter.asText(q.execSelect()));

//		Model data = load();
		
		Model data = Files
				.list(Paths.get(ttls))
				//.filter(p-> p.getFileName().toString().equals("etab-enssecqualifiant-public-men.csv.ttl"))
				.map(p -> ModelFactory.createDefaultModel().read(p.toString()))
				.reduce((m1,m2)->  ModelFactory.createUnion(m1, m2))
				.orElse(null);
		

		
		OntModel onto = createOntModel();
		onto.add(data);
		onto.write(new FileOutputStream("all_data.xml"));

		
		System.out.println("DataSize: "+data.size());

		
		// Execute every select 
		Files.list(Paths.get(selects))
			.filter(p-> { 
				System.out.println("\n\n ====== Processing SELECT :" + p+" ====== "); 
				return p.toString().endsWith(".req");
			})
			.map(path->QueryFactory.read(path.toString()))
			.forEach(query -> {
				System.out.println(query.toString(Syntax.defaultQuerySyntax));
				QueryExecution qexec = QueryExecutionFactory.create(query, ModelFactory.createRDFSModel(data)) ;
				
				ResultSet results = qexec.execSelect() ;
				System.out.println(ResultSetFormatter.asText(results));
			});
		
		// Execute every ask 
		Files.list(Paths.get(ask))
			.filter(p-> { System.out.println("\n\n ====== Processing ASK :" + p+" ====== "); return p.toString().endsWith(".req");})
			.map(path-> QueryFactory.read(path.toString()))
			.forEach(query -> {
				System.out.println(query.toString(Syntax.defaultQuerySyntax));
				QueryExecution qexec = QueryExecutionFactory.create(query,data) ;
				System.out.println("Result : "+qexec.execAsk()); 
			});

		
	}
	
	public static OntModel createOntModel() throws FileNotFoundException{
		OntModel onto = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		OntClass schoool = onto.createClass("http://schema.org/School");
		OntClass mschoool = onto.createClass("http://schema.org/MiddleSchool");
		OntClass hschoool = onto.createClass("http://schema.org/HighSchool");
		OntClass univ = onto.createClass("http://schema.org/CollegeOrUniversity");
		OntClass educ  = onto.createClass("http://schema.org/EducationalOrganization");
		
		educ.addSubClass(schoool);
		educ.addSubClass(mschoool);
		educ.addSubClass(hschoool);
		educ.addSubClass(univ);
		
		
		OntProperty name = onto.createOntProperty("http://schema.org/name");
		name.addRange(literal);
		name.addDomain(educ);
		
		OntProperty streetAdress = onto.createOntProperty("http://schema.org/streetAddress");
		streetAdress.addRange(literal);
		streetAdress.addDomain(educ);
		
		OntProperty tel = onto.createOntProperty("http://schema.org/telephone");
		tel.addRange(literal);
		tel.addDomain(educ);
		
		OntProperty loc = onto.createOntProperty("http://schema.org/addressLocality");
		loc.addRange(literal);
		loc.addDomain(educ);
		
		OntProperty reg = onto.createOntProperty("http://schema.org/addressRegion");
		reg.addRange(literal);
		reg.addDomain(educ);
		
		OntProperty pub = onto.createOntProperty("http://www.data.gov.ma/data/fr/group/education/ontology.owl#isPublic");
		pub.addRange(bool);
		pub.addDomain(educ);
		
		loc.addSameAs(onto.createResource("http://dbpedia.org/ontology/city"));
		tel.addSameAs(onto.createResource("http://dbpedia.org/property/phoneNumber"));
		name.addSameAs(onto.createResource("http://dbpedia.org/property/name"));
		streetAdress.addSameAs(onto.createResource("http://dbpedia.org/ontology/address"));
		
		onto.write(new FileOutputStream("onto.ttl"),"TURTLE");
		
		return onto;
	}
	
}
