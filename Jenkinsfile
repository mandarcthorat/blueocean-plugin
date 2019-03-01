#!groovy

if (JENKINS_URL == 'https://ci.jenkins.io/') {
    buildPlugin(
      platforms: ['linux'],
      tests: [skip: true]
    )
    return
}

// only 20 builds
properties([buildDiscarder(logRotator(artifactNumToKeepStr: '20', numToKeepStr: '20'))])

weeklyAth = ['2.138.4']
if (env.JOB_NAME =~ 'blueocean-weekly-ath') {
  weeklyAth.add('2.121.1')
  weeklyAth.add('2.150.3')
}

node() {
  sauce('saucelabs') {
    stage('Setup') {
      deleteDir()
      checkout scm
      sh 'docker build -t blueocean_build_env --build-arg GID=$(id -g ${USER}) --build-arg UID=$(id -u ${USER}) - < Dockerfile.build'
      withCredentials([file(credentialsId: 'blueoceandeploy_ath', variable: 'FILE')]) {
        sh 'mv $FILE acceptance-tests/live.properties'
      }
      configFileProvider([configFile(fileId: 'blueocean-maven-settings', variable: 'MAVEN_SETTINGS')]) {
        sh 'mv $MAVEN_SETTINGS settings.xml'
      }
      withCredentials([file(credentialsId: 'blueocean-ath-private-repo-key', variable: 'FILE')]) {
        sh 'mv $FILE acceptance-tests/bo-ath.key'
      }
      sh "./acceptance-tests/runner/scripts/start-sc.sh"
      sh "./acceptance-tests/runner/scripts/start-bitbucket-server.sh"
    }

    try {
      docker.image('blueocean_build_env').inside("--net=container:blueo-selenium") {
        withEnv(['GIT_COMMITTER_EMAIL=me@hatescake.com','GIT_COMMITTER_NAME=Hates','GIT_AUTHOR_NAME=Cake','GIT_AUTHOR_EMAIL=hates@cake.com']) {
          ip = sh(returnStdout: true, script: "hostname -I  | awk '{print \$1}'").trim()
          echo "IP: [${ip}]"

          stage('Sanity check dependencies') {
            sh "node ./bin/checkdeps.js"
            sh "node ./bin/checkshrinkwrap.js"
          }

          stage('Building JS Libraries') {
            sh 'node -v && npm -v'
            sh 'npm --prefix ./js-extensions run build'
          }

          stage('Building BlueOcean') {
            timeout(time: 90, unit: 'MINUTES') {
              sh "mvn clean install -V -B -DcleanNode -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -Dmaven.test.failure.ignore -s settings.xml -Dmaven.artifact.threads=30"
            }

            junit '**/target/surefire-reports/TEST-*.xml'
            junit '**/target/jest-reports/*.xml'
            jacoco execPattern: '**/target/jacoco.exec', classPattern : '**/target/classes', sourcePattern: '**/src/main/java', exclusionPattern: 'src/test*'
            archive '*/target/code-coverage/**/*'
            archive '*/target/*.hpi'
            archive '*/target/jest-coverage/**/*'
          }

          weeklyAth.each { version ->
            stage("ATH - Jenkins ${version}") {
              withEnv(["webDriverUrl=https://${env.SAUCE_USERNAME}:${env.SAUCE_ACCESS_KEY}@ondemand.saucelabs.com/wd/hub","saucelabs=true", "TUNNEL_IDENTIFIER=${env.BUILD_TAG}"]) {
                timeout(time: 90, unit: 'MINUTES') {
                  dir('acceptance-tests') {
                    sh "bash -x ./run.sh -v=${version} --host=${ip} --no-selenium --settings='-s ${env.WORKSPACE}/settings.xml'"
                    junit './target/surefire-reports/*.xml'
                    archive './target/screenshots/**/*'
                    saucePublisher()
                  }
                }
              }
            }
          }
        }
      }
    } catch(err) {
      echo(err)
      currentBuild.result = "FAILURE"

      if (err.toString().contains('exit code 143')) {
        currentBuild.result = "ABORTED"
      }
    } finally {
      stage('Cleanup') {
        sh "${env.WORKSPACE}/acceptance-tests/runner/scripts/stop-sc.sh"
        sh "${env.WORKSPACE}/acceptance-tests/runner/scripts/stop-bitbucket-server.sh"
        deleteDir()
      }
    }
  }
}

