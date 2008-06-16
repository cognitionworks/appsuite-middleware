#!/usr/bin/perl
package Listcontext;
use strict;
use Data::Dumper;
use base qw(BasicCommandlineOptions);


#Use the SOAP::Lite Perl module
#use SOAP::Lite +trace => 'debug';
use SOAP::Lite;

my $test = new Listcontext();
$test->doRequest();

sub new {
	my ($inPkg) = @_;
	my $self = BasicCommandlineOptions->new();
	
	bless $self, $inPkg;
    return $self;
}

sub doRequest {
   	my $inSelf = shift;
    my $soap = SOAP::Lite->ns( $inSelf->{'serviceNs'} )->proxy( $inSelf->{'basisUrl'}."OXContextService" );
    
    my $pattern = SOAP::Data->value("*");
    my $creds = SOAP::Data->type("Credentials")->value(\SOAP::Data->value(SOAP::Data->name("login" => "oxadminmaster"),SOAP::Data->name("password" => "secret")));
    
    my $som_entry = 
      $soap->list($pattern,$creds);
    
    if ( $som_entry->fault() ) {
        $inSelf->fault_output($som_entry);
    } else {
        my $fields = [ "id", "filestore_id", "filestore_name", "enabled", "max_quota", "used_quota", "name", "lmappings" ]; 
        
        my @results = $som_entry->paramsall;
        #print @results[0];
        
        my @data = $inSelf->SUPER::fetch_results($fields, \@results);
        
        $inSelf->doCSVOutput($fields, \@data);
    }

}

exit;

