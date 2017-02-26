# SearchBenchmarking
 
 Solr vs ElasticSearch
 
 
## Setting up Solr :
 
Use Schema from resource folder : schema.xml
 
Use SolrConfig from the resource folder :solrconfig.xml
 


## Initialization :

http://localhost:9000/solr/admin/cores?action=CREATE&name=coreX&instanceDir=benchmarktest1&config=solrconfig.xml&schema=schema.xml&dataDir=data
