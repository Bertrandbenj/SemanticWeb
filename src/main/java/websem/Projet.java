package websem;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

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
		
		
//		QueryExecutionFactory
//		.create(
//				QueryFactory.read(transform+"req.sparql"),
//				ModelFactory.createDefaultModel().read(inputs+"etab-ensseccollegial-public.csv"))
//		.execConstruct()
//		.write(System.out);

//		Model data = load();
		
		Model data = Files
				.list(Paths.get(ttls))
				//.filter(p-> p.getFileName().toString().equals("etab-enssecqualifiant-public-men.csv.ttl"))
				.map(p -> ModelFactory.createDefaultModel().read(p.toString()))
				.reduce((m1,m2)->  ModelFactory.createUnion(m1, m2))
				.orElse(null);

		OntModelSpec s = new OntModelSpec( OntModelSpec.OWL_MEM );
		s.setDocumentManager( new OntDocumentManager("onto.xml") );
		OntModel onto = ModelFactory.createOntologyModel( s ,data );
		
		//onto.write(new FileOutputStream("all_data.xml"));
		
		
//		InfModel inf = ModelFactory.createInfModel(
//				null,  
//				ModelFactory.createDefaultModel().read("onto.xml"),
//				data);
		
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
				QueryExecution qexec = QueryExecutionFactory.create(query,onto) ;
				
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
}
