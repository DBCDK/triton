#!groovy

def workerNode = "devel8"

pipeline {
	agent {label workerNode}
	tools {
		// refers to the name set in manage jenkins -> global tool configuration
		maven "Maven 3"
	}
	environment {
		DOCKER_TAG = "${env.BRANCH_NAME}-${env.BUILD_NUMBER}"
		GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
	}
	triggers {
		pollSCM("H/03 * * * *")
	}
	options {
		timestamps()
	}
	stages {
		stage("clear workspace") {
			steps {
				deleteDir()
				checkout scm
			}
		}
		stage("verify") {
			steps {
				sh "mvn verify pmd:pmd javadoc:aggregate"
				junit "target/surefire-reports/TEST-*.xml"
			}
		}
		stage("warnings") {
			agent {label workerNode}
			steps {
				warnings consoleParsers: [
					[parserName: "Java Compiler (javac)"],
					[parserName: "JavaDoc Tool"]
				],
					unstableTotalAll: "0",
					failedTotalAll: "0"
			}
		}
		stage("pmd") {
			agent {label workerNode}
			steps {
				step([$class: 'hudson.plugins.pmd.PmdPublisher',
					  pattern: '**/target/pmd.xml',
					  unstableTotalAll: "1",
					  failedTotalAll: "1"])
			}
		}
		stage("docker build") {
			steps {
				script {
					def image = docker.build("docker-io.dbc.dk/triton-service:${env.DOCKER_TAG}",
						"--pull --no-cache .")
					image.push()
				}
			}
		}
		stage("update staging version") {
			agent {
				docker {
					label workerNode
					image "docker.dbc.dk/gitops-deploy-env"
					alwaysPull true
				}
			}
			when {
				branch "master"
			}
			steps {
				dir("deploy") {
					git(url: "gitlab@gitlab.dbc.dk:metascrum/triton-deploy.git", credentialsId: "gitlab-meta",
						branch: "staging", poll: false)
					sh """
						set-new-version triton-dbckat-service.yml ${env.GITLAB_PRIVATE_TOKEN} 143 ${env.DOCKER_TAG} -b staging
					"""
				}
			}
		}
	}
}
