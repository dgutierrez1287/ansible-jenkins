#!groovy

/* 
    This script will ensure an ssh key secret is present or absent and the 
    the content is up to date
*/

import hudson.model.*
import jenkins.model.*
import jenkins.*
import hudson.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*


// Main()
def credentialsStore = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
def domain = Domain.global()

def oldSshCredential = null
def credentialExists = false

// check if credential exists if it does set the flag true and save the current credential
println("Checking for ssh key secret with id ${secretId}")
credentialsStore?.getCredentials(domain).each{
    if (it.id == "${secretId}") {
        credentialExists = true
        oldSshCredential = it
        println("${secretId} found")
    }
}

// if the secret state is present, create or update
if ("${secretState}" == "present") {
    // if found check the private key files are the same
    if (credentialExists) {
        // if the key path is the same there is no update to run
        if ("${sshKeyPath}" == oldSshCredential.getPrivateKeySource().getPrivateKeyFile()) {
            println("Ssh file for secret ${secretId}  hasn't changed, nothing to update")
        }
        // an update to the credential is needed 
        else {
            println("Updating the credential ${secretId} the key path is different")

            newSshCredential = new BasicSSHUserPrivateKey(
                CredentialsScope.GLOBAL,
                "${secretId}",
                "${sshUsername}",
                new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource("${sshKeyPath}"),
                "",
                "${secretId} SSH secret generated by ansible"
            )

            credentialsStore.updateCredentials(domain, oldSshCredential, newSshCredential)       
        }
    }
    // since the credential doesn't exist, create it
    else {
        println("Creating the credential ${secretId} since it doesn't exist")

        sshCredential = new BasicSSHUserPrivateKey(
            CredentialsScope.GLOBAL,
            "${secretId}",
            "${sshUsername}",
            new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource("${sshKeyPath}"),
            "",
            "SSH secret generated by ansible"
        )

        credentialsStore.addCredentials(domain, sshCredential)
    } 
} 
// remove the secret if it exists
else if ("${secretState}" == "absent") {
    // if the credential exists, remove it
    if (credentialExists) {
        println("Removing ssh credential ${secretId}")
        credentialsStore.removeCredentials(domain, oldSshCredential)
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

