package websem;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

public class TP {

	public static void main(String[] args) {
		Model model = ModelFactory.createDefaultModel().read("rdfsTP.xml");
		
		InfModel inf = ModelFactory.createRDFSModel(model); 
		
		Query query = QueryFactory.create(
				"PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"+ 
				"PREFIX sch:  <http://schema.org/>\n"+
				"PREFIX ex:  <http://ex.org/a#>\n"+
				"PREFIX xml:  <http://www.w3.org/XML/1998/namespace>\n"+
				"PREFIX u: <http://www.example.org/uni#>\n"+
				"SELECT ?fn ?ln \n"+
				"WHERE { \n"+
				"	?s a u:painter ."+
				"   ?s u:first_name ?fn ."+
				"  	?s u:last_name ?ln"+
				"}");
		QueryExecution qexec = QueryExecutionFactory.create(query, inf) ;
		ResultSet results = qexec.execSelect() ;
		while (results.hasNext())
		{
			QuerySolution binding = results.nextSolution();
			Resource ln = (Resource) binding.get("ln");
			Resource fn = (Resource) binding.get("fn");
		    System.out.println("Subject: "+ln + "  "+ fn);
		}
		
	}

}
