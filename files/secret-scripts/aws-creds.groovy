#!groovy

/* 
    This script will ensure an aws credentials secret is present or absent and the 
    the content is up to date
*/

import hudson.model.*
import jenkins.model.*
import jenkins.*
import hudson.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl

def fixNull(val){
    if (val == null){
        return ""
    }
    else {
        return val
    }
}

//Main()
def credentialsStore = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
def domain = Domain.global()

def oldCredential = null
def credentialExists = false

// check if credential exists if it does set the flag true and save the current credential
println("Checking for aws credentials secret with id ${secretId}")
credentialsStore?.getCredentials(domain).each{
    if (it.id == "${secretId}") {
        credentialExists = true
        oldCredential = it
        println("${secretId} found")
    }
}

// if the secret state is present, create or update
if ("${secretState}" == "present") {
    // if found check if username and password are the same
    if (credentialExists) {
        // if acccess key, secret key and iam role are the same no update needed 
        if ((fixNull("${accessKey}") == oldCredential.getAccessKey()) && (fixNull("${secretKey}") == oldCredential.getSecretKey().getPlainText()) && (fixNull("${iamRoleArn}") == oldCredential.getIamRoleArn()) ) {
            println("access key, secret key and iam role arn for secret ${secretId} hasn't changed, nothing to update")
        }
        // an update is needed
        else {
            println("Updating the credential ${secretId} access key, secret key or iam role is different")

            newCredential = new AWSCredentialsImpl(
                CredentialsScope.GLOBAL,
                "${secretId}",
                "${accessKey}",
                "${secretKey}",
                "${secretId} AWS credential secret generated by ansible",
                "${iamRoleArn}",
                ""
            )

            credentialsStore.updateCredentials(domain, oldCredential, newCredential)
        }
    }
    // since the credential doesn't exist create it
    else {
        println("Creating credential ${secretId} since it doesn't exist")

        credential = new AWSCredentialsImpl(
           CredentialsScope.GLOBAL,
            "${secretId}",
            "${accessKey}",
            "${secretKey}",
            "${secretId} AWS credential secret generated by ansible",
            "${iamRoleArn}",
            "" 
        )

        credentialsStore.addCredentials(domain, credential)
    }
}
// remove the secret if it exists
else if ("${secretState}" == "absent") {
    // if the credential exists remove it 
    if (credentialExists) {
        println("Removing aws credential ${secretId}")
        credentialsStore.removeCredentials(domain, oldCredential)
    }
    // the credential doesn't exist, no action
    else {
        println("No credential found ${secretId}, nothing to remove")
    }
}
// error out since the secret state isn't correct
else {
    println("The state of the secret must be present or absent")
}